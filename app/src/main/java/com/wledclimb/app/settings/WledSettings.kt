package com.wledclimb.app.settings

import kotlinx.coroutines.flow.Flow

/**
 * Persists the WLED controller's address across app restarts.
 *
 * An interface (rather than the DataStore class directly) so ViewModels that
 * depend on it can be unit tested - DataStore needs a real Android `Context`,
 * which isn't available in a local JVM test.
 */
interface WledSettings {

    /** The saved controller address, or null if setup hasn't been completed yet. */
    val wledIp: Flow<String?>

    suspend fun saveWledIp(ip: String)
}
