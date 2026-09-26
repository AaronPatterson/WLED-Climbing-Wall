package com.wledclimb.app.network

/**
 * The controller answered but did not say who it is.
 *
 * Its own type rather than an [java.io.IOException] because the two need
 * different things from whoever is reading the message: one means check the
 * network, this means the controller is not one the app can keep routes for.
 * Reporting it as unreachable would send someone to look at their Wi-Fi for a
 * wall that is answering perfectly well.
 */
class WledIdentityException(message: String) : Exception(message)
