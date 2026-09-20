package com.wledclimb.app.grid

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Parses the panel layout out of WLED's raw `/json/cfg` response.
 *
 * @throws WledConfigException if the response has no 2D matrix layout in it,
 *   which is what a 1D WLED setup - or a device that isn't WLED at all -
 *   looks like from here.
 */
fun parsePanels(rawConfig: String): List<Panel> = try {
    val panels = JSONObject(rawConfig)
        .getJSONObject("hw")
        .getJSONObject("led")
        .getJSONObject("matrix")
        .getJSONArray("panels")

    (0 until panels.length()).map { i ->
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
} catch (e: JSONException) {
    throw WledConfigException("This controller isn't set up as a 2D matrix in WLED.", e)
}

/**
 * Parses WLED's `/2d-gaps.json` - a flat JSON array of -1 (missing pixel),
 * 0 (inactive pixel), or 1 (active pixel), one entry per cell in the matrix's
 * bounding box, in row-major order.
 *
 * @throws WledConfigException if the file isn't a flat list of numbers. A
 *   malformed gap file is surfaced rather than ignored: silently falling back
 *   to "no gaps" would render a grid that looks right but has every LED index
 *   after the first gap silently wrong.
 */
fun parseGaps(rawGaps: String): List<Int> = try {
    val array = JSONArray(rawGaps)
    (0 until array.length()).map { i -> array.getInt(i) }
} catch (e: JSONException) {
    throw WledConfigException("The controller's gap file (/2d-gaps.json) isn't a valid list of numbers.", e)
}
