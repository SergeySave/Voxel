package com.sergeysav.voxel.common.region

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.pool.LocalObjectPool
import com.sergeysav.voxel.common.pool.ObjectPool
import com.sergeysav.voxel.common.pool.with
import com.sergeysav.voxel.common.world.World
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
    private val world: ()->World<Chunk>,
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
    private val regionPosPool: ObjectPool<MutableRegionPosition> = LocalObjectPool({ MutableRegionPosition() }, 5)

    override fun run() {
        alive = true

        while (alive) {
            do { // Add regions
                val added = addQueue.poll()?.also { position ->
                    val region = regions.find { it.position == position }
                    if (region == null) {
                        regions.add(Region(position, regionFileSource(position), world()))
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
        // Only add it to the queue if we don't know that it is loaded
        if (tryGetRegion(pos) == null) {
            addQueue.put(pos)
        }
    }

    fun requestRegionLoad(regionPosition: RegionPosition) {
        val pos = regionPosPool.get()
        pos.set(regionPosition)
        // Only add it to the queue if we don't know that it is loaded
        if (tryGetRegion(pos) == null) {
            addQueue.put(pos)
        }
    }

    fun tryGetRegion(regionPosition: RegionPosition): Region? {
        for (i in 0 until regions.size) {
            regions.getOrNull(i)?.let {
                if (it.position == regionPosition) {
                    return it
                }
            }
        }
        return null
    }

    fun tryGetRegion(chunkPosition: ChunkPosition): Region? = regionPosPool.with {
        it.setToRegionOf(chunkPosition)
        tryGetRegion(it)
    }

    fun getRegion(regionPosition: RegionPosition): Region {
        var region = tryGetRegion(regionPosition)
        if (region == null) {
            do {
                requestRegionLoad(regionPosition)
                region = tryGetRegion(regionPosition)
            } while (region == null)
        }
        return region
    }

    fun getRegion(chunkPosition: ChunkPosition): Region = regionPosPool.with {
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