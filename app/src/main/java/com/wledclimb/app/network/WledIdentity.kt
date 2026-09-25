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
 * [mac] is empty when a controller does not report one. WLED itself falls back
 * to reading eFuse directly rather than publishing zeros, so this should not
 * happen - but callers treat an empty value as "no usable identity" rather
 * than as an identity that several walls could share.
 */
data class WledIdentity(val name: String, val mac: String) {

    val hasStableId: Boolean get() = mac.isNotBlank()
}
