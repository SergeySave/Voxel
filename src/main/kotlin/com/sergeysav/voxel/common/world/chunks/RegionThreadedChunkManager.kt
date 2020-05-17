package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.queuing.ChunkQueueingThread
import com.sergeysav.voxel.common.chunk.queuing.ChunkQueuingStrategy
import com.sergeysav.voxel.common.region.RegionManagerThread
import com.sergeysav.voxel.common.world.World
import com.sergeysav.voxel.common.world.generator.ChunkGenerator
import mu.KotlinLogging
import java.nio.channels.FileChannel
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * @author sergeys
 *
 * @constructor Creates a new RegionThreadedChunkManager
 */
class RegionThreadedChunkManager<C : Chunk>(
    loadingStrategy: ChunkQueuingStrategy<C>,
    savingStrategy: ChunkQueuingStrategy<C>,
    chunkGenerator: ChunkGenerator<in Chunk>,
    internalQueueSize: Int = 64,
    loadingParallelism: Int = 1,
    savingParallelism: Int = 1,
    regionFilesBasePath: String
) : ChunkManager<C> {

    private val log = KotlinLogging.logger { }
    private lateinit var world: World<C>
    private lateinit var chunkReleaseCallback: (C) -> Unit
    private lateinit var chunkLoadedCallback: (C) -> Unit

    private val loadingQueue = ChunkQueueingThread(loadingStrategy, internalQueueSize, "Chunk Loading Queue Thread", ::removeFromLoadCallback)
    private val savingQueue = ChunkQueueingThread(savingStrategy, internalQueueSize, "Chunk Saving Queue Thread")
    private val regionManager = RegionManagerThread({ pos ->
        Files.createDirectories(FileSystems.getDefault().getPath("$regionFilesBasePath/region.${pos.x}.${pos.y}.${pos.z}.vrf").parent)
        FileChannel.open(
            FileSystems.getDefault().getPath("$regionFilesBasePath/region.${pos.x}.${pos.y}.${pos.z}.vrf"),
            StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE
        )
    }, this::world, internalQueueSize, "Region Manager Thread")

    private val loadingThreads = Array(loadingParallelism) { ChunkLoadingThread(
        loadingQueue.output, chunkGenerator, regionManager, ::loadedCallback,
        this::world, "Chunk Loading Thread $it"
    ) }
    private val savingThreads = Array(savingParallelism) { ChunkSavingThread(
        savingQueue.output, regionManager, ::savedCallback, savingQueue::addToQueue, "Chunk Saving Thread $it"
    ) }

    private fun removeFromLoadCallback(chunk: C) {
        when (chunk.state) {
            Chunk.State.DYING -> {
                savingQueue.addToQueue(chunk)
            }
            Chunk.State.LOADING, Chunk.State.EMPTY -> {
                chunk.state = Chunk.State.DEAD // Cancel any loading
                regionManager.tryGetRegion(chunk.position)?.loadedChunks?.remove(chunk.position)
                chunkReleaseCallback(chunk)
            }
            else -> {
                log.error { "A non-dying, non-loading chunk was removed from the loading queue." }
            }
        }
    }

    private fun loadedCallback(chunk: C) {
        if (chunk.state == Chunk.State.LOADING) {
            chunk.state = Chunk.State.ALIVE
            chunkLoadedCallback(chunk)
        }
    }

    private fun savedCallback(chunk: C) {
        if (chunk.state == Chunk.State.FINALIZING) {
            chunk.state = Chunk.State.DEAD
            regionManager.tryGetRegion(chunk.position)?.loadedChunks?.remove(chunk.position)
            chunkReleaseCallback(chunk)
        }
    }

    override fun initialize(world: World<C>, chunkReleaseCallback: (C) -> Unit, chunkLoadedCallback: (C) -> Unit) {
        log.trace { "Initializing Chunk Manager" }
        this.world = world
        this.chunkReleaseCallback = chunkReleaseCallback
        this.chunkLoadedCallback = chunkLoadedCallback

        loadingQueue.start()
        savingQueue.start()
        regionManager.start()
        for (thread in loadingThreads) {
            thread.start()
        }
        for (thread in savingThreads) {
            thread.start()
        }
    }

    override fun requestLoad(chunk: C) {
        if (chunk.state != Chunk.State.EMPTY) {
            log.error { "Requested to load a non-empty chunk. An error has likely occurred." }
        }
        loadingQueue.addToQueue(chunk)
    }

    override fun requestUnload(chunk: C) {
        // Unload can be called multiple times, so we only want to respond if the chunk is in the right state:
        // A chunk must be either loading or alive to begin the unloading procedure
        if (chunk.state != Chunk.State.ALIVE && chunk.state != Chunk.State.LOADING) return

        val debounce = chunk.state != Chunk.State.DYING
        chunk.state = Chunk.State.DYING // This should act to cancel the chunk in the loading threads
        if (debounce) { // Prevent this from being called too many times
            loadingQueue.removeFromQueue(chunk)
        }
    }

    override fun notifyChunkDirty(chunk: C) {
        // We only want to try to save a chunk that is currently alive, if it isn't that means the change
        // occurred during chunk generation (so it's ok if we don't save it)
        if (chunk.state != Chunk.State.ALIVE) return
        savingQueue.addToQueue(chunk)
    }

    override fun <T : BlockState> setUnloadedChunkBlock(
        chunkPosition: ChunkPosition,
        localPosition: BlockPosition,
        block: Block<T>,
        blockState: T
    ) {
        regionManager.getRegion(chunkPosition).changeChunkMeta(chunkPosition, localPosition, block, blockState)
    }

    override fun update() {
        // We don't need to do anything on the main thread
    }

    override fun cleanup() {
        loadingQueue.cancel()
        savingQueue.cancel()
        regionManager.cancel()
        for (thread in loadingThreads) {
            thread.cancel()
        }
        for (thread in savingThreads) {
            thread.cancel()
        }
    }
}