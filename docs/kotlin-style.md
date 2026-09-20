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
- The one place per screen that's allowed to depend on a ViewModel is its "Route" — currently the corresponding branch of the `when` in `MainActivity`'s `setContent { }` (e.g. the `RootUiState.NeedsSetup` branch for `SetupScreen`). That's where `viewModel()`/`viewModels()`, `collectAsState()`, and `LaunchedEffect` live; everything below it takes hoisted state and lambdas.

## Coroutines

- Launch coroutines from `viewModelScope` — never a manually created `CoroutineScope` or `GlobalScope`. Ties the coroutine's lifetime to the ViewModel automatically.
- A `suspend fun` that does blocking I/O (network, disk) should wrap the actual blocking call in `withContext(Dispatchers.IO) { ... }`, as in `WledClient.getOn()`/`getConfig()`.
- Only mark a function `suspend` if it genuinely suspends (calls another suspend function) — don't add it just because a class happens to deal with coroutines elsewhere.

## Logging & comments

- One `private const val TAG = "ClassName"` per file that logs, declared at file scope (not inside the class body).
- Comments explain *why*, not *what* — the code already says what it does. Only comment a non-obvious constraint, a workaround, or something a reader would otherwise miss (see the `"v":true` note in `WledClient.setOn()`).

## File organization

- One class/interface per file, file name matching the type name — including small general-purpose helpers like `LambdaViewModelFactory`, which gets its own file rather than living inside whichever class happened to need it first.
- An extension property/function that logically belongs to a type it doesn't own (`Context.dataStore`) goes at file scope in the file that uses it, marked `private` unless other files genuinely need it too.
