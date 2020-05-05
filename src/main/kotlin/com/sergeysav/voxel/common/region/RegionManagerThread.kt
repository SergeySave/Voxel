package com.sergeysav.voxel.common.region

import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.pool.LocalObjectPool
import com.sergeysav.voxel.common.pool.ObjectPool
import com.sergeysav.voxel.common.pool.with
import mu.KotlinLogging
import java.nio.channels.FileChannel
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new RegionManagerThread
 */
class RegionManagerThread(
    private val regionFileSource: (RegionPosition)->FileChannel,
    addRemoveQueueSize: Int,
    name: String
) : Thread() {

    private var alive = false
    private val log = KotlinLogging.logger {  }

    init {
        this.name = name
        this.isDaemon = true
    }

    private val regions = ArrayList<Region>(10)
    private var index = 0

    private val addQueue: BlockingQueue<MutableRegionPosition> = ArrayBlockingQueue(addRemoveQueueSize)
//    private val removeQueue: BlockingQueue<MutableRegionPosition> = ArrayBlockingQueue(addRemoveQueueSize)
    private val regionPosPool: ObjectPool<MutableRegionPosition> = LocalObjectPool({ MutableRegionPosition() }, 5)

    override fun run() {
        alive = true

        while (alive) {
            do { // Add regions
                val added = addQueue.poll()?.also { position ->
                    val region = regions.find { it.position == position }
                    if (region == null) {
                        regions.add(Region(position, regionFileSource(position)))
                    } else {
                        regionPosPool.put(position)
                    }
                }
            } while (added != null)
            for (i in regions.lastIndex downTo 0) {
                if (regions[i].loadedChunks.size == 0) {
                    regions[i].emptyFor++
                    if (regions[i].emptyFor == 100) {
                        regions.removeAt(i)
                    }
                } else {
                    regions[i].emptyFor = 0
                }
            }
            if (index >= regions.size) {
                index = 0
            }
            try {
                if (regions.size > 0) {
                    regions[index++].trySaveOldChunkMetas()
                }
            } catch (e: Exception) {
                log.error(e) { "An error occurred trying to save old chunk metas" }
            }
        }
    }

    fun requestRegionLoad(chunkPosition: ChunkPosition) {
        val pos = regionPosPool.get()
        pos.setToRegionOf(chunkPosition)
        addQueue.put(pos)
    }

    fun getRegion(regionPosition: RegionPosition): Region? {
        for (i in 0 until regions.size) {
            regions.getOrNull(i)?.let {
                if (it.position == regionPosition) {
                    return it
                }
            }
        }
        return null
    }

    fun getRegion(chunkPosition: ChunkPosition): Region? = regionPosPool.with {
        it.setToRegionOf(chunkPosition)
        getRegion(it)
    }

    fun cancel() {
        alive = false
        for (region in regions) {
            region.cleanup()
        }
    }
}