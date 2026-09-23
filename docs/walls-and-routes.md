# Walls and routes

The domain model behind phase 4. For how these are navigated between, see
[navigation.md](navigation.md); for what happens when two people use the wall at
once, see [wall-sharing.md](wall-sharing.md).

## Requirements

- A saved route can be selected and edited **without being connected to the
  wall**.
- The app stores a wall: its dimensions and gap pattern. That is what makes
  offline editing possible, and it means a new route can be created with no
  controller present.
- Selecting a route does not apply it to the wall, even when the wall is on.
  Applying is a deliberate action.
- The stored wall has to be checked against the real one on reconnect, because it
  can change - a new gap file, a resized matrix.
- A wall changing must not silently destroy routes built against the old layout.
- Future: more than one wall. Routes are linked to a wall from the start so that
  this does not need a migration later.
- Future: marking a route read-only so it cannot be changed by accident, with the
  flag removable when an edit is intended.

## What this changes beneath the surface

Two of those requirements invert assumptions the app is currently built on.

**A wall stops being something fetched and becomes something stored.** Today
`Wall` is rebuilt from `/json/cfg` on every launch, and `WallUiState` has exactly
three shapes - `Connecting`, `Connected`, `Error` - with no way to express
"showing a route while disconnected". Offline editing needs the layout persisted,
and needs disconnection to stop being an error.

**Applying becomes explicit.** Every hold tap currently pushes the whole route to
the controller. Edit locally, apply deliberately is a different model, and it is
the one that makes offline editing coherent.

## Fingerprinting

Each wall stores a fingerprint of its dimensions and gap pattern, and every route
records the fingerprint it was created against. On reconnect the app recomputes
from `/json/cfg` and `/2d-gaps.json` and compares.

Routes that no longer match are **kept, not discarded**. They carry a warning in
the list, and opening one diffs its lit positions against the current gap pattern
so the holds that have gone can be shown and the route repaired.

What a wall stores is therefore occupancy, not wiring. `Wall` used to record
where each LED sat along the physical strip, ported from WLED's own
`setUpMatrix()`; that turned out to be read by nothing, because per-pixel
commands address grid positions and WLED applies its own ledmap when rendering.
The strip indices and the serpentine walk that produced them are gone, which is
what leaves a wall small enough to be worth storing: width, height, and which
cells hold a light.

## What a stored wall does not keep

The grid is stored as `holdGrid`: one character per cell, row-major, '1' where a
hold can be lit. That is the gap file *resolved* against panel coverage and
flattened to a yes or no, and it is deliberately not reversible. WLED's -1 (no
LED wired) and 0 (an LED wired but unused) both land on '0', and only the second
consumes a strip index, so a gap file rebuilt from `holdGrid` would shift every
LED after the first gap.

The raw gap values are therefore **not** stored. Phase 12's gap editor refetches
`/2d-gaps.json` when it opens, which costs nothing: it has to be connected to
upload the result anyway, and a fresh copy beats a possibly stale one. This is
also why the column is not named after the gap file - a name suggesting it could
be written back would be an invitation to that bug.

## Open questions

- **What "last selected route" survives.** Process death, certainly. Whether it
  survives switching walls is a different question.
