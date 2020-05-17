package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.region.RegionManagerThread
import com.sergeysav.voxel.common.world.World
import com.sergeysav.voxel.common.world.generator.ChunkGenerator
import mu.KotlinLogging
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new ChunkProcessingThread
 */
class ChunkLoadingThread<C : Chunk>(
    private val processingQueue: BlockingQueue<C>,
    private val generator: ChunkGenerator<in C>,
    private val regionManagerThread: RegionManagerThread,
    private val callback: (C)->Unit,
    private val worldSource: ()-> World<C>,
    name: String
) : Thread() {

    private var alive = false

    init {
        this.name = name
        this.isDaemon = true
    }

    override fun run() {
        alive = true

        while (alive) {
            val c = processingQueue.take()
            if (c.state != Chunk.State.EMPTY) continue

            c.state = Chunk.State.LOADING // Set loading state

            val region = regionManagerThread.getRegion(c.position)

            if (c.state != Chunk.State.LOADING) {
                continue
            }

            // Increment the number of loaded chunks
            region.loadedChunks.add(c.position)
            region.tryLoadChunk(c)

            if (c.state != Chunk.State.LOADING) continue

            if (!c.generated) {
                generator.generateChunk(c, worldSource())
                if (c.state != Chunk.State.LOADING) continue
                c.generated = true
            }

            callback(c)
        }
    }

    fun cancel() {
        alive = false
    }

    companion object {
        private val log = KotlinLogging.logger {  }
    }
}