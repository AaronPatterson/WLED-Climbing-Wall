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

**Chosen: B.** The primary device is a tablet in landscape, which is what
list-detail is for, and it is the only option where selecting a route does not
cost the list. Material 3's `NavigationSuiteScaffold` gives a rail on a tablet
and a bottom bar on a phone from one implementation, so phones are not a second
layout.

Wall controls sit in a top bar spanning the whole app in every option, which is
what satisfies "reachable from most screens" without repeating them per screen.
Configuration screens are pushed as full destinations, which is what takes the
controls away there.

## Open questions

- **Control and takeover.** Detection is achievable; a lock is not, without
  abusing controller state. Decide which before building apply.
- **Management vs configuration.** Management is wall-scoped (gaps, picture) and
  configuration is app-scoped (address, about). Once there is more than one
  wall, management belongs underneath a wall rather than beside settings.
- **What "last selected route" survives.** Process death, certainly. Whether it
  survives switching walls is a different question.
