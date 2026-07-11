# M1 Interview Prep — Domain Contracts

Questions and model answers for Milestone 1 (inference + chat domain contracts).

> **How to use this file:** answer each question out loud *before* reading the
> answer. If your answer and the written one disagree, figure out which is
> wrong before moving on. Memorized answers collapse under follow-ups; owned
> reasoning doesn't.

---

## Architecture

### 1. Walk me through what happens, layer by layer, when a user sends a message.

The Compose UI calls a ViewModel function with the typed text. The ViewModel
wraps it in a `ChatMessage(role = USER, ...)` and calls
`ChatRepository.sendMessage(chatMessage)` — a domain interface, so
presentation never sees the data layer. The implementation
(`ChatRepositoryImpl`) appends the message to the conversation history it
owns, **translates domain vocabulary into engine vocabulary** — it builds one
prompt string from the system prompt plus the history formatted in the model's
chat template — and calls `InferenceEngine.generate(prompt)`. The engine
streams token-deltas back as `Flow<String>`; the repository forwards them; the
ViewModel collects, accumulates tokens into UI state, and persists the
completed reply as a `ChatMessage(role = MODEL, ...)`.

The key sentence: **the repository implementation is the translation seam
between conversations and prompt strings — that's where context engineering
lives** (chat template, system prompt, history truncation, and later RAG
injection).

### 2. Repository and engine have nearly identical streaming signatures. Why do both exist?

They differ where it matters: **what goes in, and who owns the work between.**
`sendMessage(ChatMessage): Flow<String>` speaks in domain vocabulary;
`generate(prompt: String): Flow<String>` speaks in raw model input. The engine
is *mechanism* — text in, tokens out; it doesn't know conversations exist.
The repository is *policy* — it owns history and prompt construction. Two swap
tests prove the seam is correctly placed: replace LiteRT with llama.cpp and no
prompt-building code moves; redesign the entire UI and no prompt-building code
moves.

### 3. Why does `InferenceEngine` live in `domain` but its implementations in `data`? What breaks if `ModelState` moves to `data`?

Dependency inversion: domain defines the contracts it needs; data supplies
implementations. Domain depends on nothing, so it stays pure Kotlin — unit
testable on the JVM, no Android framework, no `android.*` imports. If
`ModelState` moved to `data`, the `InferenceEngine` interface (domain) would
have to import from `data` to expose `StateFlow<ModelState>` — the dependency
arrow would point the wrong way and the layering collapses: presentation and
domain would both be coupled to the data layer's internals.

### 4. Why is your repository stateful, and what did that decision cost?

`sendMessage` takes only the new message; the repository holds conversation
history internally. Wins: the ViewModel stays a presentation concern, the
conversation has a single source of truth in the data layer, and M4's Room
migration is invisible to callers ("in memory" becomes "in Room" behind the
same interface). Costs: `sendMessage` is no longer pure — the same call
produces different prompts depending on prior calls — so tests must establish
state first, and "which conversation?" eventually needs an explicit
`conversationId` because a process-wide singleton can't hold "the"
conversation forever.

---

## Kotlin & Concurrency

### 5. Why does `generate()` return `Flow` but isn't `suspend`, while `load()` is `suspend` and returns nothing?

Mental model: **how many results, and when?** `load()` produces one outcome
(done or failed) after seconds of heavy work — `suspend` parks the caller
without blocking a thread and makes the wait cancellable. `generate()`
produces many results over time — that's a `Flow`. The function itself is not
`suspend` because **constructing a cold flow does zero work** — it's a recipe,
not a meal; the suspension happens inside the flow, at collection time, token
by token. A `suspend fun` returning `Flow` would claim construction takes
time, which is a lie.

### 6. `StateFlow` vs `Flow` — why is `modelState` the former and the reply stream the latter?

**State is what's true now; events are things that happen.** `StateFlow` is
hot and always holds a current value — a brand-new collector receives it
immediately. Rotation scenario: model is `Ready`, user rotates, the screen
re-collects. With `StateFlow`: instant `Ready`, send button enabled. With a
cold `Flow`: the new collector gets nothing until the state next changes —
possibly never — so the UI renders "unknown" while a loaded model sits idle.
The reply stream is the opposite case: a bounded sequence of occurrences that
must all be delivered in order and then *complete*. `StateFlow` fails it three
ways: it demands an initial value, it never completes (the UI can't know the
reply finished), and it conflates — a slow collector is only guaranteed the
latest value, so token-deltas would be silently dropped: corrupted text.

### 7. How does tapping "stop" mid-generation actually terminate native inference?

Structured concurrency end to end. The ViewModel collects the flow in a
coroutine (scoped to `viewModelScope` or a dedicated `Job`). "Stop" cancels
that job → the collector's coroutine cancels → the flow's producer scope dies
→ in a `callbackFlow`-based engine implementation, `awaitClose { }` fires —
and that block is exactly where the native engine's abort/cancel API is
called. Kotlin cancellation reaches all the way down into C++ land. Leaving
the screen cancels `viewModelScope` and takes the same path for free.

### 8. Why is `Role` an enum but `ModelState` a sealed interface?

`Role` has a fixed set of variants carrying **no per-variant data** — an enum
states that guarantee; a sealed hierarchy would advertise variance that
doesn't exist. `ModelState` variants carry *different* data (`Error` holds a
`cause: Throwable`; `Loading` may gain a progress field) — that requires
distinct types, hence sealed. The flip trigger: the moment a role must carry
its own payload — e.g. a `Tool` role holding a function-call result in Phase 3
— enum stops fitting and sealed takes over.

---

## On-Device LLM

### 9. Defend token-delta emissions over accumulated text.

Re-emitting the accumulated string is quadratic waste: by token 500 you've
shipped ~125,000 characters to deliver 500. Deltas are linear. The obligation
it creates: **the consumer accumulates** — the ViewModel appends each emission
to the current text — and the contract must be documented on the interface,
because emission semantics that live only in the author's head become bugs in
the next implementer's code.

### 10. Who owns the model, at what scope, and how do you avoid the Low Memory Killer while backgrounded?

A dedicated `@Singleton` manager owns the engine — application *scope* without
Application-class *ownership* (stuffing it in the Application class is
service-locator style: invisible dependencies, untestable, god-object drift).
The key decoupling: **Hilt owns the wrapper's lifetime; we own the weights'
lifetime.** The wrapper is a tiny immortal object exposing `load()`/`unload()`.
On background/trim-memory signals, `unload()` frees the multi-GB native
allocation while the singleton survives; the process stays skinny, so it stops
being the LMK's juiciest target. Trade-off accepted: a cold reload when the
user returns after an unload.

### 11. Why can't two screens each own an engine instance?

Two multi-GB native allocations on a ~6GB device: the second `load()` can
simply fail to allocate — and native allocation failures don't throw a
catchable Kotlin `OutOfMemoryError`; they can abort the process from the C++
side. Instant crash, no useful stack trace. Even the "successful" case wastes
gigabytes duplicating identical weights. The device physically cannot afford
two — which is *why* the engine is a process-wide singleton, not a
per-screen resource.

### 12. "LLMs are stateless" — what is conversation history really, and where is the context window enforced?

The model remembers nothing between calls. Every turn, the *entire*
conversation — system prompt + prior turns + new message — is re-sent as the
prompt. "History" is prompt construction. The context window (a few thousand
tokens on-device) bounds it, so long conversations need a strategy: sliding-
window truncation of oldest turns first, later summarization. In this
architecture that enforcement lives in exactly one place: the repository
implementation — the same seam that builds the prompt.

---

## System Design

### 13. You can't ship 3GB in an APK. Design model delivery.

- **Download**: on first run, via WorkManager — survives process death,
  constraint-aware (Wi-Fi-only option, storage check). Must be **resumable**
  (HTTP range requests): a 3GB download *will* be interrupted.
- **Integrity**: verify a checksum (SHA-256) against a manifest before
  accepting the file; a truncated or corrupted model must never reach the
  loader.
- **Capability gating**: before offering a model, check free storage and RAM;
  offer smaller variants (or refuse gracefully) on low-RAM devices.
- **UI**: explicit states — not downloaded / downloading (with progress,
  pausable) / verifying / ready / failed (with retry). The chat screen keys
  off `ModelState` and the download state; the send button is only enabled at
  `Ready`.
- **Versioning** (LLMOps on the edge): a manifest maps app version → model
  version + URL + checksum, enabling staged rollouts and rollback without an
  app update; old model files are deleted after a successful swap.

---

*Generated at the close of M1. Next set arrives at the close of M2 (fake
engine + streaming chat UI).*
