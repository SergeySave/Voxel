package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.ImmutableChunkPosition
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

        // RACE CONDITION
        // It is possible that the same chunk is added in very quick succeession to the chunk saving queue
        // Between additions a saving thread takes the chunk and begins processing it.
        // The first thread finishes, and releases the chunk - The chunk now has invalid data as it has already been
        // released
        // Thus a chunk MUST not be released until ALL threads have finished saving it
        // Note: a simple check does not work

        while (alive) {
            val c = processingQueue.take()
            var region = regionManagerThread.getRegion(c.position)
            if (region == null) {
                log.error { "Saving Thread needs to load a region: this may or may not cause a problem" }
                errorCallback(c)
                continue
            }

            if (c.needsSaving) {
                region.saveChunk(c)
            }
            callback(c)
        }
    }

    fun cancel() {
        alive = false
    }
}