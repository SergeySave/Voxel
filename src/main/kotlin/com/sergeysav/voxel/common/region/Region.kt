package com.sergeysav.voxel.common.region

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.datamapper.NaiveChunkDataMapper
import com.sergeysav.voxel.common.chunk.datamapper.ZStdChunkDataMapper
import com.sergeysav.voxel.common.pool.ConcurrentObjectPool
import com.sergeysav.voxel.common.pool.with
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * @author sergeys
 *
 * @constructor Creates a new Region
 */
class Region(
    val position: RegionPosition,
    private val fileChannel: FileChannel
) {
    val numLoadedChunks = AtomicInteger(0)
    var emptyFor = 0

    private val versionBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0L, 1L)
    private val chunkTableBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 1L, CHUNKS * 4L).order(ByteOrder.BIG_ENDIAN).asIntBuffer()
    private val chunkSectorsStart = 1 + CHUNKS * 4L
    private val chunkSectorSize = 4096L // 4 KiB
    private val sectorSortingBuffer = IntArray(CHUNKS) { it }
    private val sectorSortingBuffer2 = IntArray(CHUNKS) { 0 }

    init {
        versionBuffer.order(ByteOrder.BIG_ENDIAN)
        versionBuffer.put(0, 1.toByte())
    }

    fun tryLoadChunk(chunk: Chunk): Boolean {
        var (x, y, z) = chunk.position
        x -= position.x * SIZE
        y -= position.y * SIZE
        z -= position.z * SIZE
        val chunkDataEntry = chunkTableBuffer[x + y * SIZE + z * SIZE * SIZE]
        val chunkSector = chunkDataEntry ushr 8
        val chunkSize = chunkDataEntry and 0xFF

        // Does the chunk exist in the file or is it the default zero
        if (chunkSize == 0) return false

        // Corruption/Out of Bounds check
        if (fileChannel.size() < chunkSectorsStart + (chunkSector + chunkSize) * chunkSectorSize) return false
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
                return true
            }
        } else if (mapperId == 0 && length == 0) {
            // Chunk not yet generated
            return false
        } else {
            error("Could not read chunk data")
        }
    }

    fun saveChunk(chunk: Chunk) {
        var (x, y, z) = chunk.position
        x -= position.x * SIZE
        y -= position.y * SIZE
        z -= position.z * SIZE
        // We need to be sure that nothing else modifies the chunk table buffer while we need it
        synchronized(chunkTableBuffer) {
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

        private val mapperPool = ConcurrentObjectPool({
            ZStdChunkDataMapper(NaiveChunkDataMapper(), ZStdChunkDataMapper.CompressionLevel.BT_ULTRA2, 256)
        }, 4)
        private val tempBufferPool = ConcurrentObjectPool({
            ByteBuffer.allocateDirect(Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 256)
        }, 4)
    }
}