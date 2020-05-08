package com.sergeysav.voxel.client.world.meshing

import com.sergeysav.voxel.client.chunk.meshing.ChunkMesher
import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.client.chunk.meshing.SplittingChunkMesher
import com.sergeysav.voxel.client.world.ClientWorld
import com.sergeysav.voxel.client.world.meshing.selection.MeshSelectionStrategy
import mu.KotlinLogging
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new ThreadedMeshingManager
 */
class SimpleThreadedMeshingManager(
    private val meshSelectionStrategy: MeshSelectionStrategy,
    parallelism: Int = 1,
    meshesPerFrame: Int = 32,
    internalQueueSize: Int = meshesPerFrame,
    dirtyQueueSize: Int = 1
) : WorldMeshingManager<ClientWorld> {

    private val log = KotlinLogging.logger {  }
    private var world: ClientWorld? = null
    private var alive = true
    private val dirtyQueue: BlockingQueue<ClientChunk> = ArrayBlockingQueue(dirtyQueueSize)
    private val mesherQueue: BlockingQueue<ChunkMesher> = ArrayBlockingQueue(meshesPerFrame)
    private val meshers: Array<ChunkMesher> = Array(meshesPerFrame) { SplittingChunkMesher(mesherQueue::put) }
    private val selectionThread = MeshingSelectionThread(internalQueueSize, dirtyQueue, meshSelectionStrategy, "Mesher Selection Thread")
    private val threads = Array(parallelism) { MeshingTaskThread(mesherQueue, dirtyQueue, "Mesher Thread $it") }

    init {
        log.trace { "Initializing World Meshing Manager with $parallelism Threads, $meshesPerFrame Meshes per Thread, and $internalQueueSize Queue" }
        for (m in meshers) {
            mesherQueue.put(m)
        }
        selectionThread.start()
        threads.forEach(MeshingTaskThread::start)
    }

    override fun getQueueSize(): Int = meshSelectionStrategy.currentSize()

    override fun notifyMeshDirty(chunk: ClientChunk) {
        if (chunk.loaded) {
            selectionThread.setDirty(chunk)
        }
    }

    override fun notifyMeshUnneeded(chunk: ClientChunk) {
        selectionThread.removeDirty(chunk)
    }

    override fun updateWorldMeshing(world: ClientWorld) {
        if (world != this.world) {
            for (thread in threads) {
                thread.world = world
            }
            this.world = world
        }

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