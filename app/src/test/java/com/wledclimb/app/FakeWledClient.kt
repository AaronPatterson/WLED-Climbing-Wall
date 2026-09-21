package com.wledclimb.app

import com.wledclimb.app.network.WledClient

/** A `/json/cfg` body describing one 2x2 panel, enough to build a real Wall from. */
const val TWO_BY_TWO_CONFIG = """
    {"hw":{"led":{"matrix":{"panels":[
        {"b":false,"r":false,"v":false,"s":false,"x":0,"y":0,"h":2,"w":2}
    ]}}}}
"""

/** What a WLED controller running a plain 1D strip looks like: no "matrix" key. */
const val ONE_DIMENSIONAL_CONFIG = """{"hw":{"led":{"total":30}}}"""

/**
 * In-memory [WledClient]. Keeps ViewModel tests deterministic - no sockets, no
 * timeouts, and failures can be injected exactly where they're wanted.
 */
class FakeWledClient(
    var on: Boolean = false,
    var config: String = TWO_BY_TWO_CONFIG,
    var gaps: String? = null,
    /** When set, every call throws this instead of returning. */
    var failWith: Exception? = null
) : WledClient {

    val setOnCalls = mutableListOf<Boolean>()
    var getConfigCount = 0
        private set

    override suspend fun getOn(): Boolean {
        failWith?.let { throw it }
        return on
    }

    override suspend fun setOn(on: Boolean): Boolean {
        failWith?.let { throw it }
        setOnCalls += on
        this.on = on
        return on
    }

    override suspend fun getConfig(): String {
        failWith?.let { throw it }
        getConfigCount++
        return config
    }

    override suspend fun getGaps(): String? {
        failWith?.let { throw it }
        return gaps
    }

    /** Every wall state pushed, oldest first. */
    val pushedHolds = mutableListOf<Map<Int, String>>()
    var lastPixelCount: Int? = null
        private set

    override suspend fun setHoldColors(pixelCount: Int, lit: Map<Int, String>) {
        failWith?.let { throw it }
        lastPixelCount = pixelCount
        pushedHolds += lit
    }
}
