package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 */
interface ChunkManager<C : Chunk> {

    /**
     * Used to initialize this chunk manager with the callbacks it needs to properly interact with the world
     *
     * @param world the world that this chunk manager is managing
     * @param chunkReleaseCallback this should be called with a chunk to signify that a chunk was unloaded successfully
     * @param callback this should be called with a chunk to signify that a chunk was loaded successfully
     */
    fun initialize(world: World<C>, chunkReleaseCallback: (C) -> Unit, callback: (C)->Unit)

    /**
     * Used to request a chunk's data to be loaded
     *
     * Note: to complete this operation the callback given in the initialize method must be called with this chunk
     *
     * @param chunk the chunk whose data must be loaded
     */
    fun requestLoad(chunk: C)

    /**
     * Used to notify that a chunk's data has been changed and that the chunk needs to saved (if applicable)
     *
     * @param chunk the chunk that was modified
     */
    fun notifyChunkDirty(chunk: C)

    /**
     * Used to set an unloaded chunk's block. This will pool changes and apply them all at once at some point.
     * If this chunk is loaded in the mean time these changes will be applied to it.
     *
     * @param chunkPosition the position of the chunk that contains the block
     * @param localPosition the local position of the block to change
     * @param block the block to set it to
     * @param blockState the state of the block to set
     */
    fun <T : BlockState> setUnloadedChunkBlock(chunkPosition: ChunkPosition, localPosition: BlockPosition, block: Block<T>, blockState: T)

    /**
     * Used to request a chunk be unloaded (or if it hasn't been loaded yet, that the loading should be cancelled)
     * This should remove any chunks that are not yet loaded from the load queue, ensure that this chunk exits
     * the dirty queue (i.e. finishes saving) (if it was in it), then calls the release callback
     *
     * Note: to complete this operation the callback given in the initialize method must be called with this chunk
     *
     * @param chunk the chunk to unload
     */
    fun requestUnload(chunk: C)

    /**
     * This is used to do update anything/do anything that must happen on the main application thread
     */
    fun update()

    /**
     * This is used to cleanup any resources used by this manager
     */
    fun cleanup()
}