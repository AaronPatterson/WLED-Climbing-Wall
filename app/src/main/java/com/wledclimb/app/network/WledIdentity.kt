package com.wledclimb.app.network

/**
 * Who a controller is, read from `/json/info` in one request.
 *
 * [mac] is WLED's own `"mac"`: the WiFi MAC with the colons stripped and
 * lower-cased, taken from the chip's eFuse. It survives reboots, firmware
 * updates, a DHCP lease moving the controller to a new address, and someone
 * renaming it - which is what makes it the right thing to identify a stored
 * wall by, where the address is not.
 *
 * It identifies the *controller*, not the wall. Replacing the board gives a
 * new identity for the same physical wall, and moving one controller between
 * two walls would make them look like one. Neither is worth designing around
 * here: the first is a rare event that wants a deliberate "this is that wall"
 * action, and the second would show up as a fingerprint mismatch anyway.
 *
 * A controller that reports no MAC is refused rather than worked around. The
 * field is unconditional in WLED - `root["mac"] = escapedMac` sits outside
 * every #ifdef in serializeInfo, and the one case that could produce an empty
 * value, an ESP32 whose WiFi netif is not up, is guarded by reading the base
 * MAC from eFuse instead. So there is no known controller this rejects, and
 * carrying a second way to identify a wall to serve a case nobody has seen
 * costs a nullable column, a branch and the tests for both.
 *
 * If one ever turns up, the app says so on connect instead of quietly falling
 * back to matching on an address that DHCP is free to move.
 */
data class WledIdentity(val name: String, val mac: String)
