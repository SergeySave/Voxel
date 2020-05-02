package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.chunk.Chunk
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new LoadingSelectionThread
 */
class LoadingSelectionThread<C : Chunk>(
    queueSize: Int,
    private val loadQueue: BlockingQueue<C>,
    name: String
) : Thread() {

    private var alive = false

    init {
        this.name = name
        this.isDaemon = true
    }

    private val chunks = mutableSetOf<C>()
    private val addQueue: BlockingQueue<C> = ArrayBlockingQueue(queueSize)
    private val removeQueue: BlockingQueue<C> = ArrayBlockingQueue(queueSize)

    override fun run() {
        alive = true

        chunks.clear()
        while (alive) {
            addQueue.poll()?.let(chunks::add)
            removeQueue.poll()?.let(chunks::remove)

            val c = if (chunks.isNotEmpty()) chunks.random() else null
            if (c != null) {
                if (loadQueue.offer(c)) {
                    chunks.remove(c)
                }
            }
        }
        chunks.clear()
    }

    fun cancel() {
        alive = false
    }

    fun setLoad(chunk: C) {
        addQueue.put(chunk)
    }

    fun removeLoad(chunk: C) {
        removeQueue.put(chunk)
    }
}