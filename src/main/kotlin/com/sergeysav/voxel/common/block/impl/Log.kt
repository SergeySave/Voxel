package com.sergeysav.voxel.common.block.impl

import com.sergeysav.voxel.common.block.state.AxialBlockState
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.data.Direction

/**
 * @author sergeys
 *
 * @constructor Creates a new Log
 */
object Log : BaseBlock<AxialBlockState>("log") {

    val states = AxialBlockState.states

    override fun getStateFromSimpleValue(value: Byte): AxialBlockState = AxialBlockState.states[value.toInt() and 0xFF]

    override fun getSimpleValueForState(state: AxialBlockState): Byte = state.axis.ordinal.toByte()
}