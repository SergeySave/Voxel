package com.sergeysav.voxel.common.chunk.queuing

import com.sergeysav.voxel.common.chunk.Chunk
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit

/**
 * @author sergeys
 *
 * @constructor Creates a new ChunkQueueingThread
 */
class ChunkQueueingThread<C : Chunk>(
    private val chunkQueuingStrategy: ChunkQueuingStrategy<C>,
    addRemoveQueueSize: Int,
    name: String,
    private val removeCallback: (C) -> Unit = {},
    private val idleMillis: Long = 2
) : Thread() {

    private var alive = false

    init {
        this.name = name
        this.isDaemon = true
    }

    private val addQueue: BlockingQueue<C> = ArrayBlockingQueue(addRemoveQueueSize)
    private val removeQueue: BlockingQueue<C> = ArrayBlockingQueue(addRemoveQueueSize)
    val output: BlockingQueue<C> = SynchronousQueue()

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
            if (next != null) {
                if (output.offer(next, idleMillis, TimeUnit.MILLISECONDS)) {
                    chunkQueuingStrategy.remove(next)
                } else {
                    sleep(idleMillis)
                }
            }
        }
        chunkQueuingStrategy.clear()
        while (addQueue.isNotEmpty() || removeQueue.isNotEmpty()) {
            addQueue.clear()
            removeQueue.clear()
            sleep(100L) // Make sure that anything trying to read/write after this has been cancelled works
        }
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