package com.wledclimb.app.setup

/**
 * Why an address couldn't be saved. Carries the address so the message can name
 * it back to the user - see [WallProblem][com.wledclimb.app.wall.WallProblem]
 * for why these are types rather than strings.
 */
sealed interface SetupProblem {

    /** Nothing was typed in.  */
    data object EmptyAddress : SetupProblem

    /** Nothing answered at [address]. */
    data class Unreachable(val address: String) : SetupProblem

    /** Something answered at [address], but it isn't a WLED 2D matrix. */
    data class NotAWledMatrix(val address: String) : SetupProblem
}
