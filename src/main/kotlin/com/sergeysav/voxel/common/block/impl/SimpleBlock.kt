package com.sergeysav.voxel.common.block.impl

import com.sergeysav.voxel.common.block.state.DefaultBlockState

/**
 * @author sergeys
 *
 * @constructor Creates a new SimpleBlock
 */
abstract class SimpleBlock(unlocalizedName: String) : BaseBlock<DefaultBlockState>(unlocalizedName) {

    override fun getSimpleValueForState(state: DefaultBlockState) = 0.toByte()
    override fun getStateFromSimpleValue(value: Byte): DefaultBlockState = DefaultBlockState
}