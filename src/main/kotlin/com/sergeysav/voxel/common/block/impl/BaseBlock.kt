package com.sergeysav.voxel.common.block.impl

import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.state.BlockState

/**
 * @author sergeys
 *
 * @constructor Creates a new BaseBlock
 */
abstract class BaseBlock<T : BlockState>(
    unlocalizedName: String
) : Block<T> {

    override val unlocalizedName: String = unlocalizedName.toLowerCase()

    override fun toString(): String = this::class.simpleName ?: "Unknown Block"
}