package com.wledclimb.app.grid

import java.security.MessageDigest

/**
 * Identifies a wall's shape, so a route can tell whether the wall it was built
 * against still looks the way it did.
 *
 * Covers dimensions and which cells hold a light, and deliberately nothing
 * else. A wall renamed, or moved to a different controller address, is still
 * the same shape and its routes are still valid. A gap file edited so a hold
 * appears or disappears is not, and every route built against the old shape
 * needs looking at.
 *
 * SHA-256 truncated to 16 hex characters: short enough to store on every route
 * without thinking about it, and far past the point where an accidental
 * collision between two hand-built climbing walls is worth worrying about.
 */
val Wall.fingerprint: String
    get() {
        val canonical = buildString {
            append(width).append('x').append(height).append(':')
            for (row in cells) for (cell in row) append(if (cell) '1' else '0')
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
