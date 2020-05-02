package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.world.generator.ChunkGenerator
import com.sergeysav.voxel.common.world.loading.selection.LoadSelectionStrategy
import mu.KotlinLogging
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new SimpleThreadedChunkManager
 */
class SimpleThreadedChunkManager<C : Chunk>(
    private val loadSelectionStrategy: LoadSelectionStrategy<C>,
    chunkGenerator: ChunkGenerator<in Chunk>,
    private val parallelism: Int = 1,
    private val chunksPerFrame: Int = 16,
    private val loadQueueSize: Int = chunksPerFrame,
    private val releaseQueueSize: Int = loadQueueSize
) : ChunkManager<C> {

    private val log = KotlinLogging.logger {  }
    private val loadQueue: BlockingQueue<C> = ArrayBlockingQueue(loadQueueSize)
    private val outputQueue: BlockingQueue<C> = ArrayBlockingQueue(chunksPerFrame)
    private val releaseQueue: BlockingQueue<C> = ArrayBlockingQueue(releaseQueueSize)
    private val selectionThread = LoadingSelectionThread(loadSelectionStrategy, ::chunkReleaseCallback, loadQueueSize, loadQueue, "Chunk Manager Selection Thread")
    private val threads = Array(parallelism) { LoadingTaskThread(loadQueue, chunkGenerator, ::doCallback, "Chunk Loading Thread $it") }
    private lateinit var releaseCallback: (C) -> Unit
    private lateinit var callback: (C) -> Unit

    fun getQueueSize() = loadSelectionStrategy.currentSize()

    override fun initialize(chunkReleaseCallback: (C) -> Unit, callback: (C) -> Unit) {
        log.trace { "Initializing Chunk Manager with $parallelism Threads, $chunksPerFrame Meshes per Thread, $loadQueueSize Load Queue, and $releaseQueueSize Release Queue" }
        this.releaseCallback = chunkReleaseCallback
        this.callback = callback
        selectionThread.start()
        threads.forEach(LoadingTaskThread<C>::start)
    }

    private fun chunkReleaseCallback(chunk: C) {
        releaseQueue.put(chunk)
    }

    private fun doCallback(chunk: C) {
        outputQueue.put(chunk)
    }

    override fun requestLoad(chunk: C) {
        selectionThread.setLoad(chunk)
    }

    override fun requestUnload(chunk: C) {
        selectionThread.removeLoad(chunk)
    }

    override fun update() {
        do {
            val c: C? = outputQueue.poll()?.also(callback)
        } while (c != null)
        do {
            val c: C? = releaseQueue.poll()?.also(releaseCallback)
        } while (c != null)
    }

    override fun cleanup() {
        selectionThread.cancel()
        threads.forEach(LoadingTaskThread<C>::cancel)
    }
}
