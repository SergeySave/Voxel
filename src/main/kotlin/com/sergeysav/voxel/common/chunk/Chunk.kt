package com.sergeysav.voxel.common.chunk

import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.block.state.DefaultBlockState
import java.util.Arrays

/**
 * @author sergeys
 *
 * @constructor Creates a new Chunk
 */
open class Chunk(val position: ChunkPosition) {
    private val blocks = Array<Block<*>>(SIZE * SIZE * SIZE) { Air }
    private val states = Array<BlockState>(SIZE * SIZE * SIZE) { DefaultBlockState }

    open fun reset() {
        Arrays.fill(blocks, Air)
        Arrays.fill(states, DefaultBlockState)
    }

    fun <T : BlockState> setBlock(localPosition: BlockPosition, block: Block<T>, state: T) {
        blocks[localPosition.x + localPosition.y * SIZE + localPosition.z * SIZE * SIZE] = block
        states[localPosition.x + localPosition.y * SIZE + localPosition.z * SIZE * SIZE] = state
    }

    fun getBlock(localPosition: BlockPosition): Block<*> {
        return blocks[localPosition.x + localPosition.y * SIZE + localPosition.z * SIZE * SIZE]
    }

    fun getBlockState(localPosition: BlockPosition): BlockState {
        return states[localPosition.x + localPosition.y * SIZE + localPosition.z * SIZE * SIZE]
    }

    override fun toString(): String {
        return "${this::class.simpleName}(position=$position)"
    }

    companion object {
        const val SIZE = 16
    }
}