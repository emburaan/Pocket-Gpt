# M3 Interview Prep — Real On-Device Inference & Streaming

Questions and model answers for Milestone 3: replacing the fake engine with
real LiteRT-LM (Gemma) inference, streaming token deltas to the UI, and
hardening the stream against cancellation, concurrent sends, and failures.

> **How to use this file:** answer each question out loud *before* reading the
> answer. If your answer and the written one disagree, figure out which is
> wrong before moving on. Every answer below was earned by building, decompiling,
> or driving it on a device in this repo — cite the code (and the run) when you
> give them.

---

## The runtime

### 1. Why LiteRT-LM over MediaPipe's LLM Inference API?

MediaPipe's `tasks-genai` is now maintenance-only; Google's current on-device
path is LiteRT-LM. The decisive ergonomic win: `Conversation.sendMessageAsync`
returns a **native `Flow<Message>`**, so streaming needs no `callbackFlow`
bridge — the token stream drops straight into the same `Flow<String>` contract
the fake engine already exposed. The model is Gemma (E2B, `.litertlm`), chosen
for a <1.5 GB runtime footprint that an emulator can hold.

### 2. Walk me through the engine lifecycle.

`Engine(EngineConfig(modelPath, cacheDir))` → `initialize()` → per generation,
`createConversation().use { … }` → `sendMessageAsync(prompt).collect { … }` →
`engine.close()` on unload. `load()` runs on `Dispatchers.IO` and drives
`modelState` through `Loading → Ready` (or `Error`). Each `generate()` opens a
**fresh** `Conversation` and closes it via `.use {}` — my decision, consistent
with the contract's "each collection is a fresh generation" and with the
repository owning history.

### 3. `initialize()` — why `withContext(Dispatchers.IO)`, and how do you *know*?

Because it's **blocking, not suspending.** I initially assumed it was `suspend`;
`javap` on the API showed `public final void initialize()` — no `Continuation`
parameter, so it's an ordinary blocking JNI call. A blocking call on the main
dispatcher would jank the UI during a multi-second, multi-GB load, so
`withContext(IO)` is required, not optional. (Lesson: trace the actual signature
before guessing at concurrency behavior.)

### 4. What was the `cacheDir` bug?

XNNPack writes a weight-cache sidecar (~model size) next to the model. The app
UID can **read** `/data/local/tmp/llm/` but not **write** there, so the first
load failed with `Permission denied`. Fix: point `EngineConfig.cacheDir` at
`context.cacheDir` (an app-owned, writable dir), wired through a `@CacheDir`
qualifier. The sidecar is rebuilt on first load — slow first launch, normal
thereafter.

---

## Streaming: delta vs snapshot

### 5. Does each emitted `Message` carry a delta (the new chunk) or a cumulative snapshot (the whole reply so far)? How did you confirm it, and why does it matter?

**Delta** — each callback carries only the latest chunk. I confirmed it two
ways. Empirically: the on-device reply reads as one clean sentence; if these
were snapshots and I concatenated them, I'd see exponential duplication
("TheThe planetsplanets…"). By decompilation: the Kotlin
`JniMessageCallbackImpl.onMessage` is a pure pass-through — it parses the native
JSON into a `Message` and forwards it with no accumulation, so the contract is
set in native C++; Google's docs state the callback "contains only the latest
chunk." It matters because it dictates the assembly rule: the collector **must
concatenate** (`(old ?: "") + delta`). A snapshot stream with the same code
would corrupt every reply.

### 6. What's the risk in this assumption, and how did you guard it?

It's a **runtime contract, not a compile-time one.** If a future LiteRT-LM
switched to cumulative snapshots, the code would still compile and just duplicate
text at runtime — invisible until you stream a reply. I pinned the assumption in
a comment at the `emit()` site naming the failure mode, so the next reader (or
me, in six months) isn't rediscovering it from corrupted output.

### 7. Trace the token's full path from native code to a pixel.

Native decoder emits a chunk → JNI callback parses it into a `Message` →
`Conversation.sendMessageAsync`'s `Flow<Message>` emits it → `generate()`
extracts text (`contents.filterIsInstance<Content.Text>().joinToString("")`) and
`emit()`s a `String` delta → `ChatRepository` forwards the `Flow<String>` →
`ChatViewModel.collect` appends the delta to `streamingReply` → Compose
recomposes the streaming bubble. Four hops, one per architectural layer.

---

## Hardening the stream

### 8. Cancelling the collector stops native generation. What actually makes that happen?

Two halves. The Kotlin half is free: `emit` and `collect` are suspend points, so
cancelling the collecting coroutine throws `CancellationException` and unwinds
the flow. But the **native** decode loop is CPU-bound and won't notice a Kotlin
`Job` flag — so I catch `CancellationException` inside `generate()`, call
`conversation.cancelProcess()` to stop the native side, then **re-throw** so
cancellation stays cooperative and structured concurrency isn't broken.
Cancellation lands at the next token boundary, never mid-decode. The model stays
loaded and reusable afterward.

### 9. A user taps send while a reply is still streaming. What does the engine do, and whose job is it to handle that?

The engine's contract (`InferenceEngine.generate` KDoc) is explicit: a second
concurrent collection throws `IllegalStateException` — "one at a time; concurrent
collection does not queue." The engine *enforces* that with an
`AtomicBoolean.compareAndSet` guard released in `finally`. But a contract is a
**two-party agreement**: the engine documents and enforces its throws; the
**consumer** (the ViewModel) must honor them. My first cut didn't — it collected
the flow with no handling — so the second send crashed the coroutine.

### 10. Show me the fix, and why each piece sits where it does.

Three moves in `ChatViewModel.sendMessage`:

- **A drop-policy guard at the top** — `if (_uiState.value.isGenerating) return`.
  This is the real fix: it prevents *issuing* the second collection, honoring
  "does not queue." `isGenerating` is raised before `collect` and cleared in
  `finally`.
- **A `try` around the whole `.collect(...)` call**, not the lambda. The throw
  originates at the terminal operator, not inside the per-token lambda — a `try`
  around the lambda body (my earlier mistake) guards the one place that can't
  throw. The `catch (IllegalStateException)` sets `showError` and does **not**
  re-throw (re-throwing re-kills the coroutine you just recovered).
- **Finalize once, after `collect` returns** — not inside the lambda. The lambda
  runs once *per token*; putting the "commit the finished MODEL message" step
  there would emit one bubble per token. `finally` resets `streamingReply` so a
  mid-stream failure can't strand a half-message.

### 11. What's the difference between the per-token work and the once work, in coroutine terms?

`collect { }`'s lambda is invoked once per emission; the `collect(...)` *call* is
a suspend function that returns once, on completion — or throws once, on failure.
So: per-token appends go **inside** the lambda; the finalize and the error/cleanup
handling go **around** the call. Conflating the two is what produced both the
fragmentation bug and the uncaught-throw crash.

### 12. How did you verify the fix rather than assume it?

Drove it on the emulator: a long "count to thirty" prompt (to widen the streaming
window), then fired a second send ~2 s into the stream — the exact scenario that
used to crash. Result: no `FATAL`, app stayed foregrounded, the active stream ran
to completion as one coherent bubble, and the second send was silently dropped.
"BUILD SUCCESSFUL" isn't verification; reproducing the original failure and
watching it not happen is.

---

## System design

### 13. This is CPU inference with a growing KV cache. Where does latency come from, and what would you change first?

Fresh-`Conversation`-per-turn re-prefills the entire history every message, so
the KV cache is recomputed and latency grows with conversation length. First
mitigation would be a persistent `Conversation` (or prefix caching) so prior
turns aren't re-encoded — deferred because correctness came first. Other levers:
NPU/GPU delegate instead of CPU, a smaller quant, and history truncation in the
repository (the context-engineering seam).

---

# M3 Finale — open questions (answer these by defending them, then fill in)

> Deliberately unanswered. Write your answers here after you can defend them
> without notes.

**Q: The drop policy silently ignores a mid-stream send. Compare it against
queue and cancel-and-replace: what does each cost, what does each imply for the
user, and when would you switch?**

*(your answer)*

**Q: Your `catch` handles `IllegalStateException` but the contract also throws
`IllegalArgumentException` for a blank prompt. Why hasn't that crashed the app,
and is relying on the UI to prevent it a sound design or a latent bug?**

*(your answer)*

**Q: `load()` is called in `init` and again in `sendMessage` when state
`!= Ready`. Walk the startup timeline and explain how a fast first send can
trigger a second concurrent `load()`. What's the fix — guard the caller, or make
`load()` idempotent — and where does it belong?**

*(your answer)*

**Q: The cancellation mechanism (`cancelProcess()`) is fully wired but nothing in
the UI triggers it. Design the stop control: what state drives its visibility,
what does tapping it cancel, and what should the half-streamed reply become?**

*(your answer)*
