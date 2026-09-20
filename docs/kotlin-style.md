# Kotlin Style Guide

General formatting (indentation, import order, spacing) follows the official guides below — let Android Studio's formatter (**Code → Reformat Code**, or format-on-save) handle that automatically rather than hand-formatting:

- [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android's Kotlin style guide](https://developer.android.com/kotlin/style-guide)

The rest of this doc is project-specific: patterns already established in this codebase, so new code stays consistent with them instead of each phase inventing its own approach.

## Naming

- Classes/interfaces/objects: `PascalCase` (`WallViewModel`, `WledClient`).
- Functions/properties/parameters: `camelCase`.
- Top-level `const val`s: `SCREAMING_SNAKE_CASE`, except the per-file logging tag, which is just `TAG` by convention (see Logging below).
- A private mutable backing property exposed publicly as read-only gets a leading underscore: `_uiState` (private, mutable) / `uiState` (public, read-only) — see `WallViewModel`, `SetupViewModel`, `RootViewModel`.

## Null safety & immutability

- Prefer `val` over `var`. State changes should go through a `StateFlow` update (`_uiState.value = ...` or `.update { }`), not a mutable field being reassigned directly.
- Avoid `!!`. Use `?.`, `?:`, `as?`, or `requireNotNull()`/`checkNotNull()` with a message instead.
- Represent "no value yet" with a nullable type (`String?`), not a sentinel like an empty string or `-1`.

## State modeling

- Model a screen's possible states as a `sealed interface` (or `sealed class`) with one `data class`/`data object` per case, instead of a cluster of nullable/boolean fields. See `WallUiState`, `SetupUiState`, `RootUiState`.
- An exhaustive `when` over a sealed type needs no `else` branch. If the compiler is asking for one, that usually means the type isn't actually sealed/exhaustive — fix that instead of adding `else`.

## ViewModels

- Expose state as `private val _x: MutableStateFlow<T>` + `val x: StateFlow<T> = _x.asStateFlow()`. Never expose the mutable type publicly.
- Constructor-inject dependencies (a client, a settings repository) instead of constructing them inline in the class body — this is what makes swapping in a fake for testing possible later, even though this repo doesn't have tests yet.
- If a ViewModel's constructor takes arguments, wire it up with `LambdaViewModelFactory` (`LambdaViewModelFactory.kt`) rather than writing a one-off `ViewModelProvider.Factory` per class.
- Don't hold onto a `Context` longer than the object needs it, and prefer `applicationContext` over an Activity context when a class stores one as a field (e.g. `WledSettings`).

## Composables

- Screen-level composables (`WallScreen`, `SetupScreen`) take plain state and event lambdas as parameters (`state: WallUiState, onToggle: () -> Unit`), never a ViewModel directly. This is "state hoisting": it keeps the composable previewable and testable without standing up a real ViewModel's dependencies (a `Context`, a `DataStore`, a network client), and its signature documents exactly what it can read and do instead of exposing the ViewModel's whole API.
- The one place per screen that's allowed to depend on a ViewModel is its "Route" — a small composable per feature (`SetupRoute`, `WallRoute`), living alongside that feature's other files. That's where `viewModel()`/`viewModels()`, `collectAsState()`, and `LaunchedEffect` live; everything below it (the `Screen` composable) takes hoisted state and lambdas. Keeps `MainActivity`'s own `when` a short dispatch table — one line per state, calling out to a `Route` — instead of the ViewModel-construction and effect-wiring code piling up inline as more screens are added.

## Testing

- **Anything a ViewModel depends on gets an interface**, with the real implementation named after its mechanism (`WledSettings`/`DataStoreWledSettings`, `WledClient`/`HttpWledClient`). Without this, ViewModel tests need a real `Context` or real sockets, and stop being worth writing. Fakes live in the test source set (`FakeWledSettings`, `FakeWledClient`).
- **ViewModel tests need `MainDispatcherRule`.** `viewModelScope` dispatches on `Dispatchers.Main`, which doesn't exist in a local JVM test - without the rule every such test fails with "Module with the Main dispatcher had failed to initialize".
- **Test behavior through the public state, not internals**: drive a ViewModel with its own functions and assert on `uiState.value`. Don't reach for the private `_uiState`.
- `HttpWledClient` is tested against MockWebServer (real HTTP, faked server); everything above it is tested against fakes (no sockets, no timing). Keep that split - integration-flavored tests at the boundary, fast deterministic tests everywhere else.
- `testOptions { unitTests.isReturnDefaultValues = true }` is set because `android.util.Log` is stubbed in local unit tests and otherwise throws, failing any test covering a path that logs.

## Coroutines

- Launch coroutines from `viewModelScope` — never a manually created `CoroutineScope` or `GlobalScope`. Ties the coroutine's lifetime to the ViewModel automatically.
- **Never let `catch (e: Exception)` swallow `CancellationException`** — it extends `Exception`, so a broad catch turns "this coroutine was cancelled" into a fake failure state and breaks structured concurrency. Rethrow it first (`catch (e: CancellationException) { throw e }`) before any broad catch.
- A `suspend fun` that does blocking I/O (network, disk) should wrap the actual blocking call in `withContext(Dispatchers.IO) { ... }`, as in `WledClient.getOn()`/`getConfig()`.
- Only mark a function `suspend` if it genuinely suspends (calls another suspend function) — don't add it just because a class happens to deal with coroutines elsewhere.

## Logging & comments

- One `private const val TAG = "ClassName"` per file that logs, declared at file scope (not inside the class body).
- Comments explain *why*, not *what* — the code already says what it does. Only comment a non-obvious constraint, a workaround, or something a reader would otherwise miss (see the `"v":true` note in `WledClient.setOn()`).

## File organization

- One class/interface per file, file name matching the type name — including small general-purpose helpers like `LambdaViewModelFactory`, which gets its own file rather than living inside whichever class happened to need it first.
- Same rule for top-level screen composables (`LoadingScreen`, `SetupScreen`, `WallScreen`): each gets its own file matching its name, rather than being bundled into `MainActivity.kt`. Keeps `MainActivity.kt` scoped to the Activity itself and the "Route" wiring (see Composables above), and keeps each screen independently easy to find, preview, and test.
- An extension property/function that logically belongs to a type it doesn't own (`Context.dataStore`) goes at file scope in the file that uses it, marked `private` unless other files genuinely need it too.

## Package structure

Package by **feature**, not by architectural layer — each screen's package holds everything specific to it (its screen composable, ViewModel, and UI state), rather than scattering a feature's own files across `ui/`, `viewmodel/`, `model/`-style layer packages:

```
com.wledclimb.app/
├── MainActivity.kt            — app entry + dispatch (which Route to show)
├── RootViewModel.kt           — app-level state, not owned by any one feature
├── LambdaViewModelFactory.kt  — shared utility
├── LoadingScreen.kt           — app-level, shown before routing to a feature
├── setup/                     — everything the setup screen needs
│   ├── SetupRoute.kt          — owns SetupViewModel, wires it to SetupScreen
│   ├── SetupScreen.kt
│   ├── SetupViewModel.kt
│   └── SetupUiState.kt
├── wall/                      — everything wall control needs
│   ├── WallRoute.kt           — owns WallViewModel, wires it to WallScreen
│   ├── WallScreen.kt
│   ├── WallViewModel.kt
│   └── WallUiState.kt
├── network/                   — WLED HTTP API client, shared across features
│   └── WledClient.kt
├── settings/                  — persisted app settings, shared across features
│   └── WledSettings.kt
└── grid/                      — the Wall domain model, shared across features
    ├── Panel.kt                — one physical panel from /json/cfg
    ├── Wall.kt                 — the logical grid: cell -> LED index or empty
    ├── WallMapper.kt           — builds a Wall from panels (ports WLED's own algorithm)
    └── WledConfigParser.kt     — parses /json/cfg into Panels
```

`grid` isn't a single screen's package like `setup`/`wall` — per `docs/design.md`, `Wall` is "the core domain concept" meant to be shared by every screen that deals with the grid (Wall control today, Route Editor/Saved Routes later), so it lives alongside `network`/`settings` as shared infrastructure rather than being owned by one feature.

New features (Route Editor, Saved Routes, etc. from `docs/design.md`'s build plan) get their own package the same way, so working on one feature stays contained to one folder instead of touching several unrelated ones. Genuinely cross-feature infrastructure (a network client, a settings store, a shared design-system component) gets its own package instead of living inside whichever feature needed it first.
