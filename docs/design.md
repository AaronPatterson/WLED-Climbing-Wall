# WLED Climb App — Getting Started Plan

2026-09-19 · @Someone

## Working with Claude on this project

Since this is a new codebase and your first mobile app, the goal is to keep every change small enough that you can understand and test it before moving on.

- **Work in a git repo from day one.** Ask Claude to commit after each working increment with a clear message. This gives you checkpoints to return to if a change goes wrong, and lets you read the diff to see exactly what changed.
- **Build a "walking skeleton" first.** Before any UI polish, get the thinnest possible path working end-to-end: app connects to the WLED controller and can turn the wall on/off. This proves the hardest integration (talking to WLED) works before layering features on it.
- **Review diffs, don't just trust output.** Ask Claude to explain unfamiliar Kotlin/Android patterns as they show up (`ViewModel`, `StateFlow`, Compose, coroutines). You don't need to memorize Android — just understand what each new piece does before it becomes load-bearing.
- **Test on a real device or emulator after every phase**, not just at the end. WLED talks over your local network, so a real Android device on the same Wi-Fi as the WLED controller is the most realistic test.
- **Keep this doc as the source of truth for decisions** (architecture choices, WLED quirks you discover, open questions). Point Claude back to it in future sessions so context isn't lost between conversations.
- **Ask for options, not just answers**, when a decision affects later work (e.g. local storage format, navigation pattern) — these are cheap to change early and expensive later.

## Architecture

**Platform:** Native Android with Kotlin and Jetpack Compose. This is the standard modern stack Google recommends and has the best tooling, docs, and community support for a first app — you won't be fighting the framework while also learning it.

**App structure (MVVM):**

| Layer | Responsibility |
| --- | --- |
| UI (Compose) | Screens and widgets; renders state, sends user actions up |
| ViewModel | Holds UI state (`StateFlow`), talks to the repository, survives rotation |
| Repository | Single source of truth; decides whether data comes from WLED or local storage |
| Data — network | Talks to the WLED controller's JSON API over HTTP on the local Wi-Fi network |
| Data — local | Room database for saved routes, plus DataStore for app settings (controller IP, chosen grid layout) |

**Talking to WLED:** WLED exposes a JSON HTTP API on the device (typically `http://<wled-ip>/json`). The relevant endpoints:

- `GET /json/info` — device info, LED count, whether it's a 2D matrix
- `GET /json/cfg` — full configuration, including the 2D matrix layout and any configured LED gaps/skips
- `GET`/`POST /json/state` — read/set power, brightness, per-segment colors and effects (this is how you turn the wall on/off and light individual holds)
- `POST /json/state` with a `"ps"` (preset) field — save/apply presets (P1 feature)

The app will fetch `/json/cfg` once during setup to learn the wall's grid dimensions and hold positions, then use `/json/state` for all live control. This keeps the app a thin client over WLED's own capabilities, matching the constraint of not modifying WLED itself.

**Hold-to-LED mapping:** the core domain concept is a `Wall` — a 2D grid where each cell is either empty or maps to an LED index. A `Route` is just a sparse map of `{ledIndex -> color}` plus a name, notes, and optional preview image. This mapping is what lets the same route-editing UI work whether the grid comes from WLED's config or (later) from a photo overlay.

**Networking library:** Ktor or Retrofit, either works well with Compose/coroutines — this is a small enough decision to make once you start coding rather than up front.

## Key features and screens

| Screen | Purpose | Priority |
| --- | --- | --- |
| Setup | Enter WLED IP/DNS, test connection, pick grid spacing (1ft, 8in, 6in, offset), pull grid + gap layout from WLED | P0 |
| Home | Wall on/off switch, connection status, quick links to routes | P0 |
| Route editor | Grid view of the wall; tap a hold to toggle its light, pick its color; remembers last color chosen | P0 |
| Saved routes | List of routes with name, notes, optional preview; apply or delete | P0 |
| Route apply | Push a saved route's colors/on-off state to the wall | P0 |
| Preset save | Save a route as a native WLED preset | P1 |
| Wall photo overlay | Take/crop a photo of the wall, auto-detect hold grid, toggle holds by touching the photo | P2 |
| Light mode toggle | Switch between All Lights / Holds Only / No Holds Only | P2 |
| Route sharing | Upload/download routes to a shared server | P5 |

A couple of things worth deciding early since kids (6+) are the target user:

- **Touch targets need to be large and forgiving** — the route editor grid should favor big tappable zones over precision, especially before the photo-overlay feature exists.
- **Color picking should be simple** — a small fixed palette (matching common hold colors) rather than a full color wheel, at least for v1.

## Incremental build plan

Each phase should end with something you can run on a device and see working, before moving to the next.

| Phase | Goal | Delivers |
| --- | --- | --- |
| 0 | Walking skeleton | Blank app that connects to a hardcoded WLED IP and can turn the wall fully on/off |
| 1 | Setup screen | Enter/save WLED IP, test connection, pull `/json/cfg` and display raw grid info |
| 2 | Grid layout | Turn the raw WLED grid into a real `Wall` model; pick/confirm grid spacing; render the grid on screen |
| 3 | Route editor (live) | Tap holds to toggle color/on-off, changes push live to the wall via `/json/state` |
| 4 | Local save/load | Room database; name + notes a route; save and list saved routes |
| 5 | Apply saved route | Load a saved route and push it to the wall in one action |
| 6 | WLED presets (P1) | Save a route as a native WLED preset |
| 7 | Photo overlay (P2) | Capture/crop a wall photo, manual or auto hold detection, toggle via the photo |
| 8 | Light modes (P2) | All Lights / Holds Only / No Holds Only toggle |
| 9 | Route sharing (P5) | Upload/download routes to a shared server |

Phases 0–5 cover every P0 requirement and form a genuinely useful app on their own — that's the natural point to pause, use it on the real wall, and see what P1/P2 work actually turns out to matter.
