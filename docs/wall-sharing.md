# Sharing the wall

One child loads and edits a route while another is climbing. The app has to
tolerate that. For how routes are stored, see
[walls-and-routes.md](walls-and-routes.md).

## Requirements

- There will be a gesture to apply a route to the wall.
- Possibly a way to see that someone else has control, with an explicit action to
  take it over. Once taken over, applying a route might become automatic.

## A convention, not a lock

Worth being clear about the limits. WLED has no notion of ownership. It is a
stateless HTTP endpoint that does whatever it was last told, and nothing in the
protocol lets one device know another exists.

So this is a convention the app encodes and offers take over and release around.
Anyone is free to ignore or clear it, including from WLED's own interface. That
is a feature rather than a hole.

## Where the claim is stored

In the segment's name field, set and read through `/json/state`:

    POST {"seg":[{"id":0,"n":"wledclimb:eli"}]}   ->  {"success":true}
    GET  /json/state                              ->  seg[0].n == "wledclimb:eli"
    POST {"seg":[{"id":0,"n":""}]}                ->  cleared

Verified against the wall on WLED 16.0.1. Setting a name changes nothing about
what the LEDs display.

Chosen over two alternatives for a concrete reason. A claim file on the
controller's filesystem was the obvious idea - it is how the gap file already
works - but `/edit` returns **401** on this controller because a settings PIN is
configured, so the file API is not available to the app. `/json/state` is not
gated. Storing the claim in a preset slot would work and is readable through
`/presets.json`, but it consumes a slot and pollutes the preset list.

The name being visible in WLED's own interface is deliberate: someone at the
controller can see that the app believes a person holds it, and can clear it
without needing the app.

For what the segment name costs and the two hazards around it - a boundary change
wiping it, and presets capturing it - see "WLED behaviour worth knowing" in
[design.md](design.md).

## Open questions

- **Detection versus claim.** A claim says who *intends* to hold the wall.
  Detection - polling `/json/state` and noticing the wall no longer matches what
  this device applied - answers a different question: whether something changed
  regardless of who claimed it. They are complementary, and it is not yet decided
  whether both are needed.
- **Whether takeover makes applying automatic**, as the requirement suggests, or
  whether applying stays deliberate even when holding the wall.
