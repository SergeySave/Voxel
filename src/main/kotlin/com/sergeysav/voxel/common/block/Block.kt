package com.sergeysav.voxel.common.block

import com.sergeysav.voxel.common.block.state.BlockState

/**
 * @author sergeys
 */
interface Block<T : BlockState> {

    /**
     * This unlocalized name is used for storing this block in a file.
     *
     * This must be lowercase.
     */
    val unlocalizedName: String

    fun getStateFromSimpleValue(value: Byte): T
    fun getSimpleValueForState(state: T): Byte
}
