package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.client.world.meshing.MeshingTaskThread
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.world.generator.ChunkGenerator
import mu.KotlinLogging
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author sergeys
 *
 * @constructor Creates a new SimpleThreadedChunkManager
 */
class SimpleThreadedChunkManager<C : Chunk>(
    chunkGenerator: ChunkGenerator<in Chunk>,
    parallelism: Int = 1,
    loadQueueSize: Int = 16,
    chunksPerFrame: Int = 16
) : ChunkManager<C> {

    private val log = KotlinLogging.logger {  }
    private val loadQueue: BlockingQueue<C> = ArrayBlockingQueue(loadQueueSize)
    private val outputQueue: BlockingQueue<C> = ArrayBlockingQueue(chunksPerFrame)
    private val selectionThread = LoadingSelectionThread(loadQueueSize, loadQueue, "Chunk Manager Selection Thread")
    private val threads = Array(parallelism) { LoadingTaskThread(loadQueue, chunkGenerator, ::doCallback, "Chunk Loading Thread $it") }
    private lateinit var callback: (C) -> Unit

    override fun initialize(callback: (C) -> Unit) {
        log.trace { "Initializing Chunk Manager" }
        this.callback = callback
        selectionThread.start()
        threads.forEach(LoadingTaskThread<C>::start)
    }

    private fun doCallback(chunk: C) {
        outputQueue.put(chunk)
    }

    override fun requestLoad(chunk: C) {
        selectionThread.setLoad(chunk)
        callback(chunk)
    }

    override fun requestUnload(chunk: C) {
        selectionThread.removeLoad(chunk)
    }

    override fun update() {
        do {
            val c: C? = outputQueue.poll()
            if (c != null) {
                callback(c)
            }
        } while (c != null)
    }

    override fun cleanup() {
        selectionThread.cancel()
        threads.forEach(LoadingTaskThread<C>::cancel)
    }
}
