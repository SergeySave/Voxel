package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.MutableChunkPosition
import com.sergeysav.voxel.common.chunk.queuing.ChunkQueuingStrategy
import com.sergeysav.voxel.common.chunk.queuing.ChunkQueueingThread
import com.sergeysav.voxel.common.pool.ConcurrentObjectPool
import com.sergeysav.voxel.common.pool.ObjectPool
import com.sergeysav.voxel.common.pool.SynchronizedObjectPool
import com.sergeysav.voxel.common.pool.with
import com.sergeysav.voxel.common.region.Region
import com.sergeysav.voxel.common.region.RegionManagerThread
import com.sergeysav.voxel.common.world.World
import com.sergeysav.voxel.common.world.generator.ChunkGenerator
import mu.KotlinLogging
import java.nio.channels.FileChannel
import java.nio.file.FileSystems
import java.nio.file.StandardOpenOption
import java.util.Collections
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new RegionThreadedChunkManager
 */
class RegionThreadedChunkManager<C : Chunk>(
    loadingChunkQueuingStrategy: ChunkQueuingStrategy<C>,
    savingChunkQueuingStrategy: ChunkQueuingStrategy<C>,
    chunkGenerator: ChunkGenerator<in Chunk>,
    processingQueueSize: Int = 1,
    savingQueueSize: Int = 64,
    internalQueueSize: Int = 64,
    loadingParallelism: Int = 1,
    savingParallelism: Int = 1,
    regionFilesBasePath: String
) : ChunkManager<C> {

    private val log = KotlinLogging.logger {  }
    private val chunkProcessingQueue: BlockingQueue<C> = ArrayBlockingQueue(processingQueueSize)
    private val chunkSavingQueue: BlockingQueue<C> = ArrayBlockingQueue(savingQueueSize)
    private val loadingQueueingThread = ChunkQueueingThread(
        loadingChunkQueuingStrategy,
        internalQueueSize,
        chunkProcessingQueue,
        "Region Threaded Chunk Manager Loading Queue Thread",
        ::onUnqueue
    )
    private val savingQueueingThread = ChunkQueueingThread(
        savingChunkQueuingStrategy,
        internalQueueSize,
        chunkSavingQueue,
        "Region Threaded Chunk Manager Saving Queue Thread"
    )
    private val regionManagerThread = RegionManagerThread(
        { pos ->
            FileChannel.open(
                FileSystems.getDefault().getPath("$regionFilesBasePath/region.${pos.x}.${pos.y}.${pos.z}.vrf"),
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE
            )
        },
        16,
        "Region Threaded Region Manager Thread"
    )
    private val loadingThreads = Array(loadingParallelism) {
        ChunkLoadingThread(
            chunkProcessingQueue,
            chunkGenerator,
            regionManagerThread,
            ::loadedCallback,
            ::getWorld,
            "Chunk Loading Thread $it"
        )
    }
    private val savingThreads = Array(savingParallelism) {
        ChunkSavingThread(
            chunkSavingQueue,
            regionManagerThread,
            ::onSave,
            { c -> savingQueueingThread.addToQueue(c) },
            "Chunk Saving Thread $it"
        )
    }
    private val toRelease = Collections.synchronizedList(ArrayList<C>(256))

    private lateinit var world: World<C>
    private lateinit var releaseCallback: (C) -> Unit
    private lateinit var callback: (C) -> Unit

    private fun loadedCallback(chunk: C) {
        callback(chunk)
    }

    private fun onSave(chunk: C) {
        if (chunk in toRelease) {
            regionManagerThread.getRegion(chunk.position)?.loadedChunks?.remove(chunk.position)
            toRelease.remove(chunk)
            releaseCallback(chunk)
        }
    }

    private fun onUnqueue(chunk: C) {
        // Unload the chunk's region if it was the last chunk that was still loaded in said region
        val region = regionManagerThread.getRegion(chunk.position)
        if (region != null) {
            savingQueueingThread.addToQueue(chunk)
        }
    }

    private fun getWorld(): World<C> = world

    override fun initialize(world: World<C>, chunkReleaseCallback: (C) -> Unit, callback: (C) -> Unit) {
        log.trace { "Initializing Chunk Manager" }
        this.world = world
        this.releaseCallback = chunkReleaseCallback
        this.callback = callback

        loadingQueueingThread.start()
        savingQueueingThread.start()
        regionManagerThread.start()
        for (thread in loadingThreads) {
            thread.start()
        }
        for (thread in savingThreads) {
            thread.start()
        }
    }

    override fun requestLoad(chunk: C) {
        regionManagerThread.requestRegionLoad(chunk.position)
        loadingQueueingThread.addToQueue(chunk)
    }

    override fun notifyChunkDirty(chunk: C) {
        chunkSavingQueue.put(chunk)
    }

    override fun <T : BlockState> setUnloadedChunkBlock(chunkPosition: ChunkPosition, localPosition: BlockPosition, block: Block<T>, blockState: T) {
        regionManagerThread.requestRegionLoad(chunkPosition)

        var region: Region?
        do {
            region = regionManagerThread.getRegion(chunkPosition)
        } while (region == null)
        region.changeChunkMeta(chunkPosition, localPosition, block, blockState)
    }

    override fun requestUnload(chunk: C) {
        if (chunk !in toRelease) {
            toRelease.add(chunk)
        }
        loadingQueueingThread.removeFromQueue(chunk)
    }

    override fun update() {
    }

    override fun cleanup() {
        loadingQueueingThread.cancel()
        savingQueueingThread.cancel()
        regionManagerThread.cancel()
        for (thread in loadingThreads) {
            thread.cancel()
        }
        for (thread in savingThreads) {
            thread.cancel()
        }

        chunkProcessingQueue.clear()
        chunkSavingQueue.clear()
        toRelease.clear()
    }
}