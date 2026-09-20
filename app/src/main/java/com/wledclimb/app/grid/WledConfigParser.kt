package com.wledclimb.app.grid

import org.json.JSONArray
import org.json.JSONObject

/** Parses the panel layout out of WLED's raw `/json/cfg` response. */
fun parsePanels(rawConfig: String): List<Panel> {
    val panels = JSONObject(rawConfig)
        .getJSONObject("hw")
        .getJSONObject("led")
        .getJSONObject("matrix")
        .getJSONArray("panels")

    return (0 until panels.length()).map { i ->
        val panel = panels.getJSONObject(i)
        Panel(
            xOffset = panel.getInt("x"),
            yOffset = panel.getInt("y"),
            width = panel.getInt("w"),
            height = panel.getInt("h"),
            bottomStart = panel.getBoolean("b"),
            rightStart = panel.getBoolean("r"),
            vertical = panel.getBoolean("v"),
            serpentine = panel.getBoolean("s")
        )
    }
}

/**
 * Parses WLED's `/2d-gaps.json` - a flat JSON array of -1 (missing pixel),
 * 0 (inactive pixel), or 1 (active pixel), one entry per cell in the matrix's
 * bounding box, in row-major order.
 */
fun parseGaps(rawGaps: String): List<Int> {
    val array = JSONArray(rawGaps)
    return (0 until array.length()).map { i -> array.getInt(i) }
}
