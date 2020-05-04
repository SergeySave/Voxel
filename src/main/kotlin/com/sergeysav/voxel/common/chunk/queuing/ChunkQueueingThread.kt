package com.sergeysav.voxel.common.chunk.queuing

import com.sergeysav.voxel.common.chunk.Chunk
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new ChunkQueueingThread
 */
class ChunkQueueingThread<C : Chunk>(
    private val chunkQueuingStrategy: ChunkQueuingStrategy<C>,
    addRemoveQueueSize: Int,
    private val processingQueue: BlockingQueue<C>,
    name: String,
    private val removeCallback: (C) -> Unit = {}
) : Thread() {

    private var alive = false

    init {
        this.name = name
        this.isDaemon = true
    }

    private val addQueue: BlockingQueue<C> = ArrayBlockingQueue(addRemoveQueueSize)
    private val removeQueue: BlockingQueue<C> = ArrayBlockingQueue(addRemoveQueueSize)

    override fun run() {
        alive = true

        chunkQueuingStrategy.clear()
        while (alive) {
            do { // Add everything to the queue
                val added = addQueue.poll()?.also(chunkQueuingStrategy::add)
            } while (added != null)
            do { // Remove everything from the queue
                val added = removeQueue.poll()?.also {
                    chunkQueuingStrategy.remove(it)
                    removeCallback(it)
                }
            } while (added != null)

            val next = chunkQueuingStrategy.tryGetNext()
            if (next != null && processingQueue.offer(next)) {
                chunkQueuingStrategy.remove(next)
            }
        }
        chunkQueuingStrategy.clear()
    }

    fun addToQueue(chunk: C) {
        addQueue.put(chunk)
    }

    fun removeFromQueue(chunk: C) {
        removeQueue.put(chunk)
    }

    fun cancel() {
        alive = false
    }
}