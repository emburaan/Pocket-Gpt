# M2 Interview Prep — Fake Engine, Contracts & DI

Model answers for the material covered in M2 so far. Every answer below was
earned by building, tracing, or debugging it in this repo — cite the code when
you give them.

---

## Cold flows

**Q: When does the code inside a `flow { }` builder run?**

Not when the function is called — `flow { }` only wraps the lambda in an object
and returns. The block runs when a terminal operator (`collect`) is invoked,
inside the collector's coroutine, and *every* collection re-runs it from the
top. Proof from this repo: a `println` as the first line of
`FakeInferenceEngine.generate()`'s block never printed when the flow was built
but not collected.

**Q: Why does that matter for API design?**

It decides *when* validation runs and *where* exceptions surface. A check
outside the braces fires at call time in the caller's plain context; a check
inside fires at collection time, inside the collector's coroutine, and travels
through the flow's error path (`.catch { }`). Our contract deliberately pins
every exception to collection time. (Also practical: `load()` is `suspend`,
`generate()` is not — the compiler itself forbids calling it outside the
braces.)

## Cooperative cancellation

**Q: `generate()` streams via `emit()` + `delay()`. Cancelling the collector
stops it mid-stream with zero cancellation code. Why?**

Kotlin cancellation is cooperative: `cancel()` only flips a flag on the `Job`.
Every suspending function from kotlinx.coroutines checks that flag when it
suspends or resumes; if set, it throws `CancellationException`, which unwinds
the coroutine (running `finally` blocks) and ends it quietly. `emit` and
`delay` are both `suspend`, so the loop has two checkpoints per iteration.

**Q: What if the loop were pure CPU work with no suspend calls?**

It would ignore cancellation entirely — no suspension point, no check. That's
what `ensureActive()` / `yield()` are for, and it becomes a real concern in M3
where native token generation is CPU-bound: cancellation lands at the next
*token boundary*, never mid-decode.

## The `generate()` contract (the auto-load decision)

**Q: What should `generate()` do if the model isn't loaded — and why not just
auto-load?**

It throws `IllegalStateException` at collection; loading is the caller's job.
Auto-load (lazy init inside the flow) was traced to three failures:

1. **Double-load race** — two collectors both see not-`Ready` and both call
   `load()`; on a real engine that's two multi-GB allocations → OOM. Checking
   for `Unloaded` instead doesn't fix it: the second collector then *skips*
   loading and generates against a model that is still `Loading` —
   use-before-ready.
2. **Error conflation** — load failures (missing file, insufficient RAM)
   surface as generation failures; the UI can't distinguish "retry download"
   from "generation hiccup."
3. **Split ownership** — the interface already exposes `modelState` and a
   public `suspend load()`; that's an API saying "caller manages lifecycle."
   Auto-load would ship a second, contradictory lifecycle manager hidden
   inside `generate()`, paid for with a mutex and wait-until-ready logic.

The caller (ViewModel via ModelManager) knows what the user is doing and owns
the moment 4GB of weights enters memory.

## `check` vs `require`

**Q: When do you use which?**

`require(cond) { msg }` throws `IllegalArgumentException` — *the caller passed
something bad* (blank prompt). `check(cond) { msg }` throws
`IllegalStateException` — *the arguments are fine but the object isn't in a
state to be asked* (model not loaded, generation already active). Guard-first
style: reject illegal input/state at the top, keep the happy path flat.
Exception messages describe the object's problem in its own voice — never a
test's expectations.

## One generation at a time

**Q: How do you enforce single-flight generation, and why not a plain
`Boolean`?**

A plain `if (busy) throw; busy = true` is a check-then-act race: two coroutines
on different dispatchers can both pass the check before either sets the flag.
`AtomicBoolean.compareAndSet(false, true)` fuses check-and-claim into one
atomic step — the loser gets `false` and the `check(...)` wrapping it throws
`IllegalStateException`. The claim comes *after* the other guards (a failed
`require` must not strand the flag), and release lives in `finally` so it runs
on all three exits: normal completion, failure, and `CancellationException`
unwinding out of a suspension point. (This repo uses
`kotlin.concurrent.atomics.AtomicBoolean` — the KMP-ready stdlib API, still
`@ExperimentalAtomicApi`; `java.util.concurrent.atomic` is the stable
alternative.)

## Contract tests

**Q: What's the difference between the test you write to learn and the test
you commit?**

An *experiment* proves one fact once, to a human (the println/collect-order
test), then dies. A *contract guardian* is a KDoc clause translated into
executable form and re-proven on every build — five tests, one per clause:
throws before load, throws on blank prompt, throws on concurrent collection,
streams after load, reusable after completion. When M3's real engine replaces
the fake, the same suite interrogates it. Guardians assert **type and timing,
never message text** — pinning prose makes tests break on rewording while
guarding nothing.

**Q: Why is "reusable after completion" the most protective test?**

It only fails if the busy flag is claimed and never released — the bug that
demos never show (everything works once) and production always finds.

## Hilt / DI

**Q: What's the difference between `@Binds` and `@Inject constructor`?**

They answer different questions. `@Binds` answers *which type*: "when someone
asks for `InferenceEngine`, hand them `FakeInferenceEngine`." `@Inject
constructor` answers *built how*: "here's the recipe." A `@Binds` whose
implementation Dagger can't construct is `[Dagger/MissingBinding]` — and the
error names both fixes: an `@Inject` constructor (canonical, couples the class
lightly to the DI framework) or a `@Provides` method (keeps the class
framework-blind, moves construction knowledge into the module).

**Q: Your bad binding compiled. Why?**

Dagger fully validates only bindings something actually *requests*. Until a
ViewModel injected `InferenceEngine`, the binding was an uncashed check —
unreachable, therefore unvalidated. Green builds verify wiring that's used,
not promises that aren't.

**Q: Why must this engine be `@Singleton`?**

It's stateful: `modelState` and the `isGenerating` flag only mean anything if
every injection point shares one instance. Proven by logging
`engine.hashCode()` from two injection sites — same hash, one object. Without
the scope: ViewModel loads engine A, repository generates on engine B, B's
state is `Unloaded`, every message fails with "model not loaded" *while the
load visibly succeeded* — and two busy flags silently void the
one-generation-at-a-time clause app-wide while every (single-instance) test
stays green. For stateful classes, scope annotations are load-bearing contract
infrastructure.

## Layering

**Q: The ViewModel needs the model loaded and messages sent. Who holds the
engine?**

Three candidate doors: engine directly in the ViewModel (presentation coupled
to inference machinery, two doors to one engine); lifecycle methods on
`ChatRepository` (but model memory management is a strange claim for a *chat*
interface to make); or a dedicated `ModelManager` — single owner of
load/unload/`modelState`, pure delegation, engine never visible above the data
layer. This repo chose `ModelManager`: each dependency answers exactly one
question — "is the model ready?" (ModelManager) and "send this message"
(ChatRepository) — and the ViewModel orchestrates both.

---

# M2 Finale — open questions (answer these by building, then fill in)

> Deliberately unanswered. The design is the deliverable; write your answers
> here after defending them.

**Q: Design `ChatUiState`: what must the screen know at any instant — and
where does the in-flight streaming reply live (inside the messages list, a
separate field, or something else)? What are the recomposition consequences of
each?**

*(your answer)*

**Q: In `ChatRepositoryImpl.sendMessage`, what is the choreography — when does
the user message enter history, who assembles token deltas into the finished
`Role.MODEL` message, and when does it enter history? What does the returned
`Flow<String>` carry?*

*(your answer)*

**Q: In the ViewModel — who calls `modelManager.load()` and when? And what is
your policy when the user sends a second message mid-stream, given what the
engine's contract does to a concurrent collection?**

*(your answer)*