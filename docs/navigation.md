# App structure and navigation

Captured 2026-09-23, before phase 4. The app currently has two destinations —
setup and the wall — switched by whether a controller address is saved. That
was enough while the wall grid was the only screen. It is not enough now.

## Requirements

### Wall controls

- Power and brightness reachable from most screens, but not from configuration
  screens.

### Routes are the point of the app

Loading an existing route, editing it, and creating a new one is the main thing
anyone does here, so that is the landing experience.

- With no saved routes, drop straight into creating one.
- With saved routes, the one selected last time is selected again at launch.
- A saved route can be selected and edited **without being connected to the
  wall**.
- Selecting a route does not apply it to the wall, even when the wall is on.
  Applying is a deliberate action.

### Walls are a stored concept

- The app stores a wall: its dimensions and gap pattern. That is what makes
  offline editing possible, and it means a new route can be created with no
  controller present.
- Future: more than one wall. Routes are linked to a wall from the start so
  that this does not need a migration later.

### More than one person at once

One child loads and edits a route while another is climbing. The app has to
tolerate that.

- There will be a gesture to apply a route to the wall.
- Possibly a way to see that someone else has control, with an explicit action
  to take it over. Once taken over, applying a route might become automatic.

### Configuration

- Somewhere to change configuration. Today that is only the controller address;
  there will be more.
- Somewhere to see the version, possibly growing into an About section.

### Management

Wall-scoped setup that is not day-to-day configuration:

- Editing the gap file, in an experience much like setting a route: tap the
  positions that have holds, rather than hand-editing JSON.
- Setting a wall picture.

### Future

- Marking a route read-only so it cannot be changed by accident, with the flag
  removable when an edit is intended.

## What this changes beneath the navigation

Three of the requirements above are not about screens.

**A wall stops being something fetched and becomes something stored.** Today
`Wall` is rebuilt from `/json/cfg` on every launch and `WallUiState` has no way
to express "showing a route while disconnected" - it has Connecting, Connected
and Error. Offline editing needs the layout persisted, and disconnection to stop
being an error.

**Applying becomes explicit.** Every hold tap currently pushes the whole route to
the controller. Edit-locally, apply-deliberately is a different model, and it is
the one that makes offline editing coherent.

**The wall becomes a shared resource.** This is the hard one, and it is worth
being clear about the limits: WLED has no notion of ownership. It is a stateless
HTTP endpoint that does whatever it was last told, and nothing in the protocol
lets one device know another exists.

What is achievable is *detection*: poll `/json/state`, notice the wall no longer
matches what this device applied, and say so. A genuine lock would need shared
state the controller does not offer, unless something like a preset slot is
abused as a claim marker. Worth settling early, because it decides whether
applying a route is fire-and-forget or a negotiation.

## Navigation patterns considered

| | Shape | Verdict |
| --- | --- | --- |
| A | Bottom navigation: Routes, Wall, Setup | Simplest, but two of three destinations are used monthly and would hold prime space permanently. A bottom bar also reads as stranded across a ten-inch tablet. |
| B | List-detail with an adaptive rail | Routes listed beside the editor, both visible. Selecting a route stops being a navigation event that hides the list. |
| C | Single screen with a drawer | Scales best as management grows, but hides everything behind a hamburger, and Material 3 discourages drawers for few destinations. |

**Chosen: both, selected by window size.** A and B are not alternatives. The
Material 3 adaptive libraries switch between them from one implementation, along
two independent axes:

| | Library | Compact (phone) | Expanded (tablet) |
| --- | --- | --- | --- |
| Navigation chrome | `NavigationSuiteScaffold` | bottom bar | navigation rail |
| Content layout | `ListDetailPaneScaffold` | one pane, list to detail | list and detail side by side |

A phone therefore gets pattern A and the tablet gets pattern B, with no branching
on device type and no second layout to maintain. `ListDetailPaneScaffold` also
handles the back stack, which is the fiddly part: back from the editor returns to
the list on a phone and does nothing on a tablet, where the list never left.

Two things follow from this that are easy to get wrong:

- The switch keys off **window** width, not device. A tablet in split screen gets
  the phone layout, which is correct but means available width is the trigger
  rather than the hardware. The tablet in portrait may land in medium rather than
  expanded, so that breakpoint is a decision to make deliberately.
- The Compose BOM here is `2024.09.00` and the current release is `2026.09.00`.
  The adaptive libraries need something far newer, so that upgrade is a
  prerequisite with its own risk rather than part of the navigation work. Same
  shape as the AGP upgrade: not scope creep, just a dependency nobody had cause
  to notice until something needed it.

Versions at time of writing, all stable: navigation-suite 1.4.0, adaptive-layout
1.3.0, adaptive-navigation 1.3.0.

The earlier reasoning for preferring B on a tablet still holds: The primary device is a tablet in landscape, which is what
list-detail is for, and it is the only option where selecting a route does not
cost the list. Material 3's `NavigationSuiteScaffold` gives a rail on a tablet
and a bottom bar on a phone from one implementation, so phones are not a second
layout.

Wall controls sit in a top bar spanning the whole app in every option, which is
what satisfies "reachable from most screens" without repeating them per screen.
Configuration screens are pushed as full destinations, which is what takes the
controls away there.

## Decisions

### Control is a convention, stored in the segment name

Not a lock. The app writes a claim and offers take over and release, and anyone
is free to ignore or clear it.

The claim lives in the segment's name field, set and read through `/json/state`:

    POST {"seg":[{"id":0,"n":"wledclimb:eli"}]}   ->  {"success":true}
    GET  /json/state                              ->  seg[0].n == "wledclimb:eli"
    POST {"seg":[{"id":0,"n":""}]}                ->  cleared

Verified against the wall on WLED 16.0.1. Setting a name changes nothing about
what the LEDs display.

Chosen over the two alternatives for a concrete reason. A claim file on the
controller's filesystem was the obvious idea - it is how the gap file already
works - but `/edit` returns **401** on this controller because a settings PIN is
configured, so the file API is not available to the app. `/json/state` is not
gated. Storing the claim in a preset slot would work and is readable through
`/presets.json`, but it consumes a slot and pollutes the preset list.

The name is visible in WLED's own interface, which is a feature: someone poking
at the controller directly can see that the app believes a person holds it, and
can clear it without needing the app.

#### What the segment name normally does, and what it costs

It is a cosmetic label shown in WLED's own web interface so several segments can
be told apart - "Ceiling", "Desk". It has no effect on rendering.

Checked in the firmware rather than assumed:

- **No flash wear.** `setName` and `clearName` are heap operations
  (`p_free(name); name = nullptr`) with no `serializeConfig()` behind them, so
  the claim can be written as often as needed.
- **It does not survive a reboot.** There are no references to segment names in
  `cfg.cpp`, so the name is absent from `cfg.json`. A power cycle drops the
  claim, which is the desired behaviour - nobody holds the wall after a reboot.
  The exception is a configured boot preset that was saved while a name was set,
  which restores it.
- **Nothing changes on the wall.** Confirmed against the hardware.

#### Two hazards to build around

**A segment command that moves the boundaries wipes the name.** From `json.cpp`:

    if (elem["n"]) {
      seg.setName(name);
    } else if (start != seg.start || stop != seg.stop) {
      seg.clearName();
    }

Any command that changes `start` or `stop` without also carrying `"n"` clears it
silently. The current `setHoldColors` sends `{"seg":{"i":[...]}}` with no bounds
and is therefore safe, but this is precisely the sort of thing that breaks later
when a field is added, so the claim has to be re-sent by anything touching
segment geometry.

**Presets capture the name.** It is serialized when `forPreset`, so saving a
route as a WLED preset bakes in whoever held the wall at that moment, and
applying that preset later restores a stale claim. This matters for phase 6 and
is a reason to strip or overwrite the name when applying a preset.

### A stored wall is fingerprinted, and routes remember which version they fit

The wall can change underneath the app - a new gap file, a resized matrix - and
routes built against the old layout may reference holds that no longer exist.

Each wall stores a fingerprint of its dimensions and gap pattern, and every route
records the fingerprint it was created against. On reconnect the app recomputes
from `/json/cfg` and `/2d-gaps.json` and compares.

Routes that no longer match are **kept, not discarded**. They carry a warning in
the list, and opening one diffs its lit positions against the current gap pattern
so the holds that have gone can be shown and the route repaired.

### Management lives under configuration for now

Gap editing and the wall picture sit inside configuration rather than becoming a
top-level destination. They can be promoted later; the point for now is to keep
the surface the children see as small as possible.

This has a consequence for the navigation above. With management folded in there
are only two top-level destinations, Routes and Settings, and Settings is the
rare one. A bottom bar carrying two items where children should only ever touch
one is worse than a single icon in the top bar, so `NavigationSuiteScaffold` is
**not** used yet - settings is a pushed destination, which is also what takes the
wall controls away from it. `ListDetailPaneScaffold` is still used, because the
list-beside-editor behaviour is the part that earns its keep.

The navigation suite goes back in when management is promoted.

## Open questions

- **What "last selected route" survives.** Process death, certainly. Whether it
  survives switching walls is a different question.
