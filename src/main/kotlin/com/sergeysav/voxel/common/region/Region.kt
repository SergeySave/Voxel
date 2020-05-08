package com.sergeysav.voxel.common.region

import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.block.state.DefaultBlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.MutableChunkPosition
import com.sergeysav.voxel.common.chunk.datamapper.NaiveChunkDataMapper
import com.sergeysav.voxel.common.chunk.datamapper.ZStdChunkDataMapper
import com.sergeysav.voxel.common.pool.ConcurrentObjectPool
import com.sergeysav.voxel.common.pool.with
import com.sergeysav.voxel.common.world.World
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * @author sergeys
 *
 * @constructor Creates a new Region
 */
class Region(
    val position: RegionPosition,
    private val fileChannel: FileChannel,
    private val world: World<Chunk>
) {
    var emptyFor = 0

    private val versionBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0L, 1L)
    private val chunkTableBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 1L, CHUNKS * 4L).order(ByteOrder.BIG_ENDIAN).asIntBuffer()
    private val chunkSectorsStart = 1 + CHUNKS * 4L
    private val chunkSectorSize = 4096L // 4 KiB
    private val sectorSortingBuffer = IntArray(CHUNKS) { it }
    private val sectorSortingBuffer2 = IntArray(CHUNKS) { 0 }
    val loadedChunks: MutableList<ChunkPosition> = Collections.synchronizedList(ArrayList<ChunkPosition>(1024))
    private val metaUpdates = ArrayList<MetaUpdate>(16)
    private val chunkUpdates = ArrayList<MutableChunkPosition>(16)

    init {
        versionBuffer.order(ByteOrder.BIG_ENDIAN)
        versionBuffer.put(0, 1.toByte())
    }

    private fun loadChunkInner(chunk: Chunk, sync: Boolean) {
        var (x, y, z) = chunk.position
        x -= position.x * SIZE
        y -= position.y * SIZE
        z -= position.z * SIZE

        val chunkDataEntry = if (sync) {
            synchronized(chunkTableBuffer) {
                chunkTableBuffer[x + y * SIZE + z * SIZE * SIZE]
            }
        } else {
            chunkTableBuffer[x + y * SIZE + z * SIZE * SIZE]
        }
        val chunkSector = chunkDataEntry ushr 8
        val chunkSize = chunkDataEntry and 0xFF

        // Does the chunk exist in the file or is it the default zero
        if (chunkSize == 0) {
            chunk.generated = false
            return
        }

        // Corruption/Out of Bounds check
        if (fileChannel.size() < chunkSectorsStart + (chunkSector + chunkSize) * chunkSectorSize) {
            chunk.generated = false
            return
        }
        val chunkBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            chunkSectorsStart + chunkSector * chunkSectorSize,
            chunkSize * chunkSectorSize
        )
        chunkBuffer.order(ByteOrder.BIG_ENDIAN)
        val length = chunkBuffer.int
        val mapperId = chunkBuffer.int
        if (mapperId == 1) {
            val newBuffer = chunkBuffer.slice()
            newBuffer.limit(length)
            mapperPool.with {
                it.readToChunk(newBuffer.slice(), chunk)
                return
            }
        } else if (mapperId == 0 && length == 0) {
            // Chunk not yet generated
            chunk.generated = false
            return
        } else {
            error("Could not read chunk data")
        }
    }

    fun tryLoadChunk(chunk: Chunk) {
        loadChunkInner(chunk, true)
        synchronized(metaUpdates) {
            applyMetaUpdatesToChunk(chunk)
        }
    }

    private fun applyMetaUpdatesToChunk(chunk: Chunk) {
        for (i in metaUpdates.size - 1 downTo 0) {
            if (metaUpdates[i].chunkPosition == chunk.position) {
                chunk.setBlock(metaUpdates[i].localPosition, metaUpdates[i].block, metaUpdates[i].state)
                metaUpdatePool.put(metaUpdates.removeAt(i))
            }
        }
    }

    private fun saveChunkInner(chunk: Chunk) {
        var (x, y, z) = chunk.position
        x -= position.x * SIZE
        y -= position.y * SIZE
        z -= position.z * SIZE

        val chunkDataEntry = chunkTableBuffer[x + y * SIZE + z * SIZE * SIZE]
        var chunkSector = chunkDataEntry ushr 8
        var chunkSize = chunkDataEntry and 0xFF
        val needsSector = chunkSize == 0

        tempBufferPool.with {
            it.rewind()
            it.limit(it.capacity())
            val len = mapperPool.with { mapper ->
                mapper.writeFromChunk(it.slice(), chunk)
            }
            val desiredChunkSize = ((len + 8) / chunkSectorSize + 1).toInt()
            if ((len + 8) > chunkSize * chunkSectorSize) { // If we need more space
                // Pick a new sector and size
                chunkSize = desiredChunkSize
                // Is the next bit free for me to simply expand into it or not?
                if (!isRunFree(chunkSector + chunkSize, chunkSector + desiredChunkSize) || needsSector) {
                    chunkSector = findFirstFreeRun(chunkSize)
                }
            } else if ((len + 8) < (chunkSize - 1) * chunkSectorSize) { // If we can shrink
                // Drop excess sectors
                chunkSize = desiredChunkSize
            }

            val chunkBuffer = fileChannel.map(
                FileChannel.MapMode.READ_WRITE,
                chunkSectorsStart + chunkSector * chunkSectorSize,
                chunkSize * chunkSectorSize
            )
            chunkBuffer.putInt(len) // Write the length
            chunkBuffer.putInt(1) // We used Mapper Number 1
            it.limit(len)
            chunkBuffer.put(it)
            it.rewind()
            it.limit(it.capacity())
            Unit
        }

        chunkTableBuffer.put(x + y * SIZE + z * SIZE * SIZE, (chunkSector shl 8) or (chunkSize and 0xFF))
    }

    fun saveChunk(chunk: Chunk) {
        // We need to be sure that nothing else modifies the chunk table buffer while we need it
        synchronized(chunkTableBuffer) {
            saveChunkInner(chunk)
        }
    }

    fun <T : BlockState> changeChunkMeta(chunkPosition: ChunkPosition, localPosition: BlockPosition, block: Block<T>, state: T) {
        val metaUpdate = metaUpdatePool.get()
        metaUpdate.chunkPosition.set(chunkPosition)
        metaUpdate.localPosition.set(localPosition)
        @Suppress("UNCHECKED_CAST")
        metaUpdate.block = block as Block<BlockState>
        metaUpdate.state = state
        metaUpdate.age = 0
        synchronized(metaUpdates) {
            metaUpdates.add(metaUpdate)
        }
    }

    fun trySaveOldChunkMetas() {
        synchronized(chunkUpdates) {
            chunkUpdates.clear()
            synchronized(metaUpdates) {
                for (i in metaUpdates.size - 1 downTo 0) {
                    metaUpdates[i].age++
                    if (metaUpdates[i].age > 1000 && metaUpdates[i].chunkPosition in loadedChunks) {
                        world.setBlock(metaUpdates[i].chunkPosition, metaUpdates[i].localPosition, metaUpdates[i].block, metaUpdates[i].state)
                        metaUpdatePool.put(metaUpdates.removeAt(i))
                    } else if (metaUpdates[i].age > 2000 && metaUpdates[i].chunkPosition !in chunkUpdates) {
                        val cp = chunkPosPool.get()
                        cp.set(metaUpdates[i].chunkPosition)
                        chunkUpdates.add(cp)
                    }
                }

                metaUpdateChunkPool.with { chunk ->
                    for (i in chunkUpdates.indices) {
                        val pos = chunkUpdates[i]
                        (chunk.position as MutableChunkPosition).set(pos)
                        chunk.reset()
                        synchronized(chunkTableBuffer) {
                            loadChunkInner(chunk, false)
                            applyMetaUpdatesToChunk(chunk)
                            saveChunkInner(chunk)
                        }
                        chunkPosPool.put(pos)
                    }
                }
            }
            chunkUpdates.clear()
        }
    }

    fun cleanup() {
        synchronized(chunkUpdates) {
            chunkUpdates.clear()
            synchronized(metaUpdates) {
                for (i in metaUpdates.size - 1 downTo 0) {
                    if (metaUpdates[i].chunkPosition !in chunkUpdates) {
                        val cp = chunkPosPool.get()
                        cp.set(metaUpdates[i].chunkPosition)
                        chunkUpdates.add(cp)
                    }
                }

                metaUpdateChunkPool.with { chunk ->
                    for (pos in chunkUpdates) {
                        (chunk.position as MutableChunkPosition).set(pos)
                        chunk.reset()
                        synchronized(chunkTableBuffer) {
                            loadChunkInner(chunk, false)
                            applyMetaUpdatesToChunk(chunk)
                            saveChunkInner(chunk)
                        }
                        chunkPosPool.put(pos)
                    }
                }
            }
            chunkUpdates.clear()
        }
    }

    private fun isRunFree(start: Int, end: Int): Boolean {
        for (i in 0 until CHUNKS) {
            val chunkData = chunkTableBuffer[i]
            val sectorNum = chunkData ushr 8
            val size = chunkData and 0xFF
            for (j in start until end) {
                if (j >= sectorNum && j < (sectorNum + size)) { // If this bit of the run is in this sector
                    return false // The run isn't free
                }
            }
        }
        return true
    }

    private fun findFirstFreeRun(size: Int): Int {
        doSortChunks()
        var lastValue = chunkTableBuffer[sectorSortingBuffer[0]]
        var lastSector = lastValue ushr 8
        var lastSize = lastValue and 0xFF
        if (lastSize == 0) {
            return 0 // THis can only happen if all chunks are empty
        }
        var maxPossibleStart = lastSector + lastSize
        for (i in 1 until CHUNKS) {
            lastValue = chunkTableBuffer[sectorSortingBuffer[i]]
            var thisSector = lastValue ushr 8
            val thisSize = lastValue and 0xFF
            if (thisSize == 0) thisSector = Int.MAX_VALUE
            maxPossibleStart = max(maxPossibleStart, lastSector + lastSize)

            if (maxPossibleStart + size <= thisSector) return maxPossibleStart

            lastSector = thisSector
            lastSize = thisSize
        }
        return lastSector + lastSize
    }

    private fun compareChunks(chunk1: Int, chunk2: Int): Boolean {
        var chunk1Sector = chunkTableBuffer[chunk1] ushr 8
        if ((chunkTableBuffer[chunk1] and 0xFF) == 0)
            chunk1Sector = Int.MAX_VALUE

        var chunk2Sector = chunkTableBuffer[chunk2] ushr 8
        if ((chunkTableBuffer[chunk2] and 0xFF) == 0)
            chunk2Sector = Int.MAX_VALUE

        return chunk1Sector <= chunk2Sector
    }

    // This is merge sort using compareChunks to check <=
    private fun doSortChunks(start: Int = 0, end: Int = CHUNKS) {
        if (end - start <= 1) return

        val mid = (start + end) / 2
        doSortChunks(start, mid)
        doSortChunks(mid, end)

        var lcopy = start
        var rcopy = mid
        for (i in start until end) {
            if (rcopy >= end || lcopy < mid && compareChunks(sectorSortingBuffer[lcopy], sectorSortingBuffer[rcopy])) {
                sectorSortingBuffer2[i] = sectorSortingBuffer[lcopy++]
            } else {
                sectorSortingBuffer2[i] = sectorSortingBuffer[rcopy++]
            }
        }

        System.arraycopy(sectorSortingBuffer2, start, sectorSortingBuffer, start, end - start)
    }

    companion object {
        const val SIZE = 16
        const val CHUNKS = SIZE * SIZE * SIZE
        private val log = KotlinLogging.logger {  }

        private val mapperPool = ConcurrentObjectPool({
            ZStdChunkDataMapper(NaiveChunkDataMapper(), ZStdChunkDataMapper.CompressionLevel.BT_ULTRA2, 256)
        }, 4)
        private val tempBufferPool = ConcurrentObjectPool({
            ByteBuffer.allocateDirect(Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 256)
        }, 4)
        private val metaUpdatePool = ConcurrentObjectPool({
            @Suppress("UNCHECKED_CAST")
            MetaUpdate(MutableChunkPosition(), MutableBlockPosition(), Air as Block<BlockState>, DefaultBlockState, 0)
        }, 4)
        private val metaUpdateChunkPool = ConcurrentObjectPool({
            Chunk(MutableChunkPosition())
        }, 1)
        private val chunkPosPool = ConcurrentObjectPool({ MutableChunkPosition() }, 32)
    }

    class MetaUpdate(
        val chunkPosition: MutableChunkPosition,
        val localPosition: MutableBlockPosition,
        var block: Block<BlockState>,
        var state: BlockState,
        var age: Int
    )
}