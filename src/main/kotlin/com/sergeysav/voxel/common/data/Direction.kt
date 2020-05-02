package com.sergeysav.voxel.common.data

/**
 * @author sergeys
 */
sealed class Direction(val textureAxis: Int, val relX: Int, val relY: Int, val relZ: Int) {
    abstract val opposite: Direction
    abstract val up: Direction
    abstract val left: Direction

    override fun toString(): String {
        return (this::class.simpleName ?: "")
    }

    object North : Direction(4, 1, 0, 0) {
        override val opposite = South
        override val up = Up
        override val left = West
    }
    object South : Direction(1, -1, 0, 0) {
        override val opposite = North
        override val up = Up
        override val left = East
    }
    object East : Direction(3, 0, 0, 1) {
        override val opposite = West
        override val up = Up
        override val left = North
    }
    object West : Direction(0, 0, 0, -1) {
        override val opposite = East
        override val up = Up
        override val left = South
    }
    object Up : Direction(2, 0, 1, 0) {
        override val opposite = Down
        override val up = South
        override val left = West
    }
    object Down : Direction(5, 0, -1, 0) {
        override val opposite = Up
        override val up = North
        override val left = West
    }

    companion object {
        val all = arrayOf(North, South, East, West, Up, Down)
    }
}
