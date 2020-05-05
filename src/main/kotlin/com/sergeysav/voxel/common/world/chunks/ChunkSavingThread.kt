package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.region.RegionManagerThread
import com.sergeysav.voxel.common.world.generator.ChunkGenerator
import mu.KotlinLogging
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new ChunkProcessingThread
 */
class ChunkSavingThread<C : Chunk>(
    private val processingQueue: BlockingQueue<C>,
    private val regionManagerThread: RegionManagerThread,
    private val callback: (C)->Unit,
    private val errorCallback: (C)->Unit,
    name: String
) : Thread() {

    private var alive = false
    private val log = KotlinLogging.logger {  }

    init {
        this.name = name
        this.isDaemon = true
    }

    override fun run() {
        alive = true

        while (alive) {
            val c = processingQueue.take()
            var region = regionManagerThread.getRegion(c.position)
            if (region == null) {
                log.error { "Saving Thread needs to load a region: this may or may not cause a problem" }
                errorCallback(c)
                continue
            }

            region.saveChunk(c)
            callback(c)
        }
    }

    fun cancel() {
        alive = false
    }
}