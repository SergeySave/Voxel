package com.sergeysav.voxel.common.world

import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.world.raycast.RaycastResult
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * @author sergeys
 */
interface World<out C : Chunk> {

    fun <T : BlockState> setBlock(blockPosition: BlockPosition, block: Block<T>, blockState: T)
    fun getBlock(blockPosition: BlockPosition): Block<*>?
    fun getBlockState(blockPosition: BlockPosition): BlockState?

    fun update()

    fun cleanup()
}