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

This is also the thing that makes `WallMapper`'s panel and serpentine handling
worth re-examining. The open question in [design.md](design.md) notes it is now
only used to work out which cells have holds, since per-pixel commands address
grid positions rather than strip indices. A stored, fingerprinted wall is
precisely what would make those strip indices unnecessary.

## Open questions

- **What "last selected route" survives.** Process death, certainly. Whether it
  survives switching walls is a different question.
