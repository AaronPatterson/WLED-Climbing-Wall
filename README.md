# WLED Climb

Android app for controlling a climbing wall with LED-lit holds (clear holds, LEDs behind them), driven by a [WLED](https://kno.wled.ge/) controller.

## Docs

- [`docs/requirements.md`](docs/requirements.md) — original feature brief and priorities (P0–P5), target user, constraints.
- [`docs/design.md`](docs/design.md) — best practices for this project, architecture (Kotlin + Jetpack Compose, MVVM, WLED JSON API), screens, and the incremental build plan.

## Status

**Phase 0 (walking skeleton) in progress**, on branch `phase-0-walking-skeleton`: a minimal app that connects to a hardcoded WLED controller IP and can turn the wall on/off via WLED's `/json/state` endpoint.

## Getting started (opening in Android Studio)

1. Open this folder in Android Studio.
2. This repo was scaffolded without a checked-in Gradle wrapper (the environment that generated it had no internet access to download one). Android Studio will offer to create the wrapper automatically on first open — accept the prompt (or run **File → Sync Project with Gradle Files**), and it will fetch the Gradle/AGP versions declared in `build.gradle.kts` using your own machine's network.
3. Once synced, run the `app` configuration on an emulator or device on the same Wi-Fi as your WLED controller.
4. The controller IP is currently hardcoded in `WallViewModel.kt` (`192.168.30.49`) — a real setup screen for this comes in a later phase.

Each phase of `docs/design.md`'s build plan is developed on its own branch and merged to `main` via pull request once it's working end-to-end.
