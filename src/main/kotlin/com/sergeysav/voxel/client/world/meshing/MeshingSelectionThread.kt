package com.sergeysav.voxel.client.world.meshing

import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.client.world.meshing.selection.MeshSelectionStrategy
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new MeshingSelectionThread
 */
class MeshingSelectionThread(
    queueSize: Int,
    private val dirtyQueue: BlockingQueue<ClientChunk>,
    private val meshSelectionStrategy: MeshSelectionStrategy,
    name: String
) : Thread() {

    private var alive = false

    init {
        this.name = name
        this.isDaemon = true
    }

    private val dirtyingQueue: BlockingQueue<ClientChunk> = ArrayBlockingQueue(queueSize)
    private val unDirtyingQueue: BlockingQueue<ClientChunk> = ArrayBlockingQueue(queueSize)

    override fun run() {
        alive = true

        meshSelectionStrategy.clear()
        while (alive) {
            do {
                val queueElement = unDirtyingQueue.poll()?.also(meshSelectionStrategy::remove)
            } while (queueElement != null)
            do {
                val queueElement = dirtyingQueue.poll()?.also(meshSelectionStrategy::add)
            } while (queueElement != null)

            val c = meshSelectionStrategy.tryGetNext()
            if (c != null) {
                if (dirtyQueue.offer(c)) {
                    meshSelectionStrategy.remove(c)
                }
            }
        }
        meshSelectionStrategy.clear()
    }

    fun cancel() {
        alive = false
    }

    fun setDirty(chunk: ClientChunk) {
        dirtyingQueue.put(chunk)
    }

    fun removeDirty(chunk: ClientChunk) {
        unDirtyingQueue.put(chunk)
    }
}