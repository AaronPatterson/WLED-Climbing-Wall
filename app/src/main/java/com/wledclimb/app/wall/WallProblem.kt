package com.wledclimb.app.wall

/**
 * Why the wall couldn't be shown or controlled.
 *
 * A type rather than a message string so the copy can live in strings.xml
 * (ViewModels have no Context to resolve resources with) and so tests assert
 * on what went wrong rather than on wording.
 */
sealed interface WallProblem {

    /** The controller didn't answer - network, power, or wrong address. */
    data object Unreachable : WallProblem

    /** It answered, but isn't a WLED controller with a 2D matrix set up. */
    data object NotAWledMatrix : WallProblem
}
