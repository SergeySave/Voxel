package com.sergeysav.voxel.common.block.state

import com.sergeysav.voxel.common.data.Direction

/**
 * @author sergeys
 */
open class AxialBlockState(val axis: Direction) : BlockState {
    companion object {
        init {
            Direction.ensureLoaded()
        }
        val north = AxialBlockState(Direction.North)
        val east = AxialBlockState(Direction.East)
        val south = AxialBlockState(Direction.South)
        val west = AxialBlockState(Direction.West)
        val up = AxialBlockState(Direction.Up)
        val down = AxialBlockState(Direction.Down)
        val states = listOf(west, south, up, east, north, down)
    }
}