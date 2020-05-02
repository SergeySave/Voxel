package com.sergeysav.voxel.common.block.state

import com.sergeysav.voxel.common.data.Direction

/**
 * @author sergeys
 */
open class AxialBlockState(val axis: Direction) : BlockState {
    companion object {
        val states = Direction.all.map { AxialBlockState(it) }
    }
}