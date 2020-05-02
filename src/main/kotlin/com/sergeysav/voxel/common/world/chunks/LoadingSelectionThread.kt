package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.world.loading.selection.LoadSelectionStrategy
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new LoadingSelectionThread
 */
class LoadingSelectionThread<C : Chunk>(
    private val loadSelectionStrategy: LoadSelectionStrategy<C>,
    queueSize: Int,
    private val loadQueue: BlockingQueue<C>,
    name: String
) : Thread() {

    private var alive = false

    init {
        this.name = name
        this.isDaemon = true
    }

    private val addQueue: BlockingQueue<C> = ArrayBlockingQueue(queueSize)
    private val removeQueue: BlockingQueue<C> = ArrayBlockingQueue(queueSize)

    override fun run() {
        alive = true

        loadSelectionStrategy.clear()
        while (alive) {
            addQueue.poll()?.let(loadSelectionStrategy::add)
            removeQueue.poll()?.let(loadSelectionStrategy::remove)

            val c = loadSelectionStrategy.tryGetNext()
            if (c != null) {
                if (loadQueue.offer(c)) {
                    loadSelectionStrategy.remove(c)
                }
            }
        }
        loadSelectionStrategy.clear()
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