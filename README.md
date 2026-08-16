# PocketGPT

An Android chat app that runs a large language model **entirely on-device** — no network calls, no server, no API key. Powered by [LiteRT-LM](https://ai.google.dev/edge/litert) running Google's Gemma model locally.

<img src="docs/screenshot_chat.png" alt="PocketGPT conversation list" width="320" />

## Features

- **On-device inference** — Gemma runs locally via LiteRT-LM; conversations never leave the phone.
- **Streaming responses** — tokens render as they're generated.
- **Multi-chat history** — start any number of named conversations; titles are derived automatically from the first message.
- **Persistent storage** — chats are saved locally with Room and survive app restarts.
- **Multi-turn context** — the full conversation is replayed to the model on every turn, so it remembers earlier messages in the same chat.

## Tech stack

- **UI:** Jetpack Compose, Material 3, Navigation Compose
- **Architecture:** MVVM with a domain/data/presentation split; unidirectional state via `StateFlow`
- **DI:** Hilt
- **Persistence:** Room
- **Concurrency:** Kotlin Coroutines & Flow
- **Inference:** [LiteRT-LM](https://ai.google.dev/edge/litert) running Gemma

## Architecture

```
domain/         — models, repository interfaces, InferenceEngine contract (no Android/framework deps)
data/
  inference/    — InferenceEngineImpl (LiteRT-LM), ModelManager (lifecycle owner)
  local/        — Room entities, DAOs, database
  repository/   — ChatRepositoryImpl: assembles history, persists messages, streams model replies
presentation/
  chat/         — chat screen + ViewModel for a single conversation
  conversations/— conversation list screen + ViewModel
  navigation/   — NavHost wiring the two screens together
```

The repository is the single source of truth for chat history: the UI observes it via `Flow`, and the inference engine itself holds no state between calls — every turn is given the full conversation it needs to answer.

## Running it

The model isn't bundled in the APK (it's several GB). For local development:

1. Push a `.litertlm` Gemma model file to the device, e.g. `/data/local/tmp/llm/`.
2. Point `provideModelPath()` (in `di/InferenceModule.kt`) at that path.
3. Build and run — `./gradlew installDebug`.

Requires `minSdk 26`. Inference runs on-device, so a physical device or an emulator with enough RAM is recommended for reasonable performance.
