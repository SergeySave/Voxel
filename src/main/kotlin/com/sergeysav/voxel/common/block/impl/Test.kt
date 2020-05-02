package com.sergeysav.voxel.common.block.impl

import com.sergeysav.voxel.common.block.state.AxialBlockState
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.data.Direction

/**
 * @author sergeys
 *
 * @constructor Creates a new Test
 */
object Test : BaseBlock<AxialBlockState>() {

    val states = AxialBlockState.states
}