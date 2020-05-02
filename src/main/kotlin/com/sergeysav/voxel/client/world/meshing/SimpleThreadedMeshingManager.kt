package com.sergeysav.voxel.client.world.meshing

import com.sergeysav.voxel.client.chunk.meshing.ChunkMesher
import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.client.chunk.meshing.SimpleChunkMesher
import com.sergeysav.voxel.client.world.ClientWorld
import com.sergeysav.voxel.client.world.meshing.selection.MeshSelectionStrategy
import com.sergeysav.voxel.client.world.meshing.selection.RandomMeshSelectionStrategy
import mu.KotlinLogging
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new ThreadedMeshingManager
 */
class SimpleThreadedMeshingManager(meshSelectionStrategy: MeshSelectionStrategy, parallelism: Int = 1, meshesPerFrame: Int = 32, dirtyQueueSize: Int = 16) :
    WorldMeshingManager<ClientWorld> {

    private val log = KotlinLogging.logger {  }
    private var world: ClientWorld? = null
    private var alive = true
    private val meshers: Array<ChunkMesher> = Array(meshesPerFrame) { SimpleChunkMesher() }
    private val dirtyQueue: BlockingQueue<ClientChunk> = ArrayBlockingQueue(meshesPerFrame)
    private val mesherQueue: BlockingQueue<ChunkMesher> = ArrayBlockingQueue(meshesPerFrame)
    private val selectionThread = MeshingSelectionThread(dirtyQueueSize, dirtyQueue, meshSelectionStrategy, "Mesher Selection Thread")
    private val threads = Array(parallelism) { MeshingTaskThread(mesherQueue, dirtyQueue, "Mesher Thread $it") }

    init {
        log.trace { "Initializing World Meshing Manager" }
        for (m in meshers) {
            mesherQueue.put(m)
        }
        selectionThread.start()
        threads.forEach(MeshingTaskThread::start)
    }

    override fun notifyMeshDirty(chunk: ClientChunk) {
        if (chunk.shouldRender) {
            selectionThread.setDirty(chunk)
        }
    }

    override fun notifyMeshUnneeded(chunk: ClientChunk) {
        selectionThread.removeDirty(chunk)
    }

    override fun updateWorldMeshing(world: ClientWorld) {
        if (world != this.world) {
            threads.forEach { it.world = world }
        }
        this.world = world

        for (m in meshers) {
            if (m.ready) {
                m.applyMesh()
                mesherQueue.put(m)
            }
        }
    }

    override fun cleanupWorldMeshing() {
        alive = false
        selectionThread.cancel()
        threads.forEach(MeshingTaskThread::cancel)
    }
}