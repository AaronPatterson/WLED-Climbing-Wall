package com.wledclimb.app.grid

/**
 * Where a hold sits in the grid.
 *
 * Routes are stored against these rather than against the segment indices the
 * wire protocol uses. A segment index is `x + y * width`, so it only means
 * anything alongside the width it was computed with: if a wall is rebuilt
 * wider, index 13 silently stops being (1,1) and becomes (13,0). Coordinates
 * survive that, which matters because a route outliving a change to its wall
 * is exactly the case the app has to handle rather than avoid.
 */
data class GridPosition(val x: Int, val y: Int)
