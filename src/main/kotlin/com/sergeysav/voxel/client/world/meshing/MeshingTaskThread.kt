package com.sergeysav.voxel.client.world.meshing

import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.client.chunk.meshing.ChunkMesher
import com.sergeysav.voxel.common.world.World
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new MeshingTaskThread
 */
class MeshingTaskThread(
    private val mesherQueue: BlockingQueue<ChunkMesher>,
    private val dirtyQueue: BlockingQueue<ClientChunk>,
    name: String
) : Thread() {

    var world: World<*>? = null
    private var alive = false

    init {
        this.name = name
        this.isDaemon = true
    }

    override fun run() {
        alive = true

        while (alive) {
            val w = world
            if (w != null) {
                val m = mesherQueue.take()
                val c = dirtyQueue.take()
                m.generateMesh(w, c)
            } else {
                sleep(1)
            }
        }
    }

    fun cancel() {
        alive = false
    }
}