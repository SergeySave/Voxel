package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.world.generator.ChunkGenerator
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new LoadingTaskThread
 */
class LoadingTaskThread<C : Chunk>(
    private val loadQueue: BlockingQueue<C>,
    private val chunkGenerator: ChunkGenerator<in C>,
    private val callback: (C)->Unit,
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
            val c = loadQueue.take()
            chunkGenerator.generateChunk(c)
            callback(c)
        }
    }

    fun cancel() {
        alive = false
    }
}