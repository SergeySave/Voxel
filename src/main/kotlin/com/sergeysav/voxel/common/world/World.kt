package com.sergeysav.voxel.common.world

import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.MutableChunkPosition
import com.sergeysav.voxel.common.world.raycast.RaycastResult
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * @author sergeys
 */
interface World<out C : Chunk> {

    /**
     * Set the block at a given position. This will silently fail if the chunk is not loaded.
     *
     * This method is slower than the other [setBlock] method. This method has no side-effects.
     *
     * @param T the type of the block state
     * @param blockPosition the world position of the block to set
     * @param block the block to set it to
     * @param blockState the state of the block
     */
    fun <T : BlockState> setBlock(blockPosition: BlockPosition, block: Block<T>, blockState: T)

    /**
     * Set the block at a given position. This will silently fail if the chunk is not loaded.
     *
     * This method is faster than the other [setBlock] method. This method has no side-effects.
     *
     * Precondition: [localPosition] is a local block position (i.e. 0 <= x, y, z < [Chunk.SIZE])
     *
     * @param T the type of the block state
     * @param chunkPosition the chunk that owns the block that should be set
     * @param localPosition the chunk-local position of the block to set
     * @param block the block to set it to
     * @param blockState the state of the block
     */
    fun <T : BlockState> setBlock(chunkPosition: ChunkPosition, localPosition: BlockPosition, block: Block<T>, blockState: T)

    /**
     * Sets a block at a given position (if the chunk is currently loaded) or writes to the chunk directly (allowing for
     * the modification of unloaded chunks). As this is an expensive process, this should only be used during terrain
     * generation as it is much slower than the [setBlock] method (and in most cases modified blocks should always be
     * loaded).
     *
     * @param T the type of the block state
     * @param blockPosition the world position of the block to set
     * @param block the block to set it to
     * @param blockState the state of the block
     */
    fun <T : BlockState> setBlockOrMeta(blockPosition: BlockPosition, block: Block<T>, blockState: T)

    /**
     * Get the block at a given position. This will return null if the block is not loaded.
     *
     * This is slower than the other [getBlock] method. This method has no side-effects.
     *
     * @param blockPosition the position of the block to get
     *
     * @return either the block or null (if the block is not currently loaded)
     */
    fun getBlock(blockPosition: BlockPosition): Block<*>?

    /**
     * Get the block state of a block at a given position. This will return null if the block is not loaded.
     *
     * This is slower than the other [getBlockState] method. This method has no side-effects.
     *
     * @param blockPosition the position of the block whose state to get
     *
     * @return either the state or null (if the block is not currently loaded)
     */
    fun getBlockState(blockPosition: BlockPosition): BlockState?

    /**
     * Simultaneously and atomically get the block and its state at a given position.
     * This will return null if the block is not loaded.
     *
     * This is slower than the other [getBlockAndState] method.
     * This method modifies the first entry in [blockStore] as a side effect.
     *
     * @param blockPosition the position of the block to get
     * @param blockStore an array whose first entry is set to the block at the given position
     *
     * @return either the state or null (if the block is not currently loaded)
     * The first entry in [blockStore] is set to the block at this position
     */
    fun getBlockAndState(blockPosition: BlockPosition, blockStore: Array<Block<*>>): BlockState?

    /**
     * Get the block at a given position. This will return null if the block is not loaded.
     *
     * This is faster than the other [getBlock] method. This method has no side-effects.
     *
     * Precondition: [localPosition] is a local block position (i.e. 0 <= x, y, z < [Chunk.SIZE])
     *
     * @param localPosition the chunk-local position of the block to get
     * @param chunkPosition the position of the chunk that owns the block to get
     *
     * @return either the block or null (if the block is not currently loaded)
     */
    fun getBlock(localPosition: BlockPosition, chunkPosition: ChunkPosition): Block<*>?

    /**
     * Get the block state of a block at a given position. This will return null if the block is not loaded.
     *
     * This is faster than the other [getBlockState] method. This method has no side-effects.
     *
     * Precondition: [localPosition] is a local block position (i.e. 0 <= x, y, z < [Chunk.SIZE])
     *
     * @param localPosition the chunk-local position of the block whose state to get
     * @param chunkPosition the position of the chunk that owns the block whose state to get
     *
     * @return either the state or null (if the block is not currently loaded)
     */
    fun getBlockState(localPosition: BlockPosition, chunkPosition: ChunkPosition): BlockState?

    /**
     * Simultaneously and atomically get the block and its state at a given position.
     * This will return null if the block is not loaded.
     *
     * This is faster than the other [getBlockAndState] method.
     * This method modifies the first entry in [blockStore] as a side effect.
     *
     * @param localPosition the chunk-local position of the block whose state to get
     * @param chunkPosition the position of the chunk that owns the block whose state to get
     * @param blockStore an array whose first entry is set to the block at the given position
     *
     * @return either the state or null (if the block is not currently loaded)
     * The first entry in [blockStore] is set to the block at this position
     */
    fun getBlockAndState(localPosition: BlockPosition, chunkPosition: ChunkPosition, blockStore: Array<Block<*>>): BlockState?

    fun update()

    fun cleanup()
}