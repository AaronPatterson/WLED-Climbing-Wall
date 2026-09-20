# WLED Climb

Android app for controlling a climbing wall with LED-lit holds (clear holds, LEDs behind them), driven by a [WLED](https://kno.wled.ge/) controller.

## Docs

- [`docs/requirements.md`](docs/requirements.md) — original feature brief and priorities (P0–P5), target user, constraints.
- [`docs/design.md`](docs/design.md) — best practices for this project, architecture (Kotlin + Jetpack Compose, MVVM, WLED JSON API), screens, and the incremental build plan.
- [`docs/kotlin-style.md`](docs/kotlin-style.md) — Kotlin coding conventions for this codebase: naming, state modeling, ViewModel/coroutine patterns, file organization.

## Status

**Phase 0 (walking skeleton) complete**: the app connects to a hardcoded WLED controller IP and turns the wall on/off via WLED's `/json/state` endpoint, verified against a real controller.

**Phase 1 (setup screen) complete**: on first launch, the app asks for the controller's IP/hostname and tests it by pulling `/json/cfg`. On success it saves the address via DataStore and moves straight to wall control; future launches skip setup entirely. A "Change controller" button on the wall screen goes back to setup (pre-filled with the current address) to switch to a different one at any time.

**Phase 2 (grid layout) complete**: the wall screen now parses `/json/cfg`'s panel layout into a real grid (porting WLED's own `setUpMatrix()` panel-to-LED-index algorithm, verified against a real controller's config with unit tests) and renders it as a read-only preview above the on/off toggle. Next up: Phase 3, making the grid interactive (tap a hold to toggle its color, live).

## Getting started (opening in Android Studio)

1. Open this folder in Android Studio.
2. Let Gradle sync automatically (the wrapper is checked in, so this should just work — no separate Gradle/AGP install needed). If it doesn't sync on its own, run **File → Sync Project with Gradle Files**.
3. Run the `app` configuration on an emulator or device on the same Wi-Fi as your WLED controller.
4. On first launch, enter your WLED controller's IP or hostname on the setup screen and tap **Test & save**. Later launches remember it and go straight to wall control.

Each phase of `docs/design.md`'s build plan is developed on its own branch and merged to `main` via pull request once it's working end-to-end.
