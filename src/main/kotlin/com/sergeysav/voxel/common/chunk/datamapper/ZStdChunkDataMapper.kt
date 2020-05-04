package com.sergeysav.voxel.common.chunk.datamapper

import com.sergeysav.voxel.common.chunk.Chunk
import org.lwjgl.util.zstd.Zstd
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author sergeys
 *
 * @constructor Creates a new NaiveChunkDataMapper
 */
class ZStdChunkDataMapper(
    private val wrappedMapper: ChunkDataMapper,
    private val compressionLevel: CompressionLevel,
    bufferSizePerBlock: Int
) : ChunkDataMapper {

    private val workingBuffer = ByteBuffer.allocateDirect(Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * bufferSizePerBlock)
    private val compContext = Zstd.ZSTD_createCCtx()
    private val decompContext = Zstd.ZSTD_createDCtx()

    init {
        workingBuffer.order(ByteOrder.BIG_ENDIAN)
    }

    override fun writeFromChunk(byteBuffer: ByteBuffer, chunk: Chunk): Int {
        workingBuffer.rewind()
        workingBuffer.limit(workingBuffer.capacity())
        val originalLen = wrappedMapper.writeFromChunk(workingBuffer.slice(), chunk)
        workingBuffer.limit(originalLen)

        val compressedSize = Zstd.ZSTD_compressCCtx(compContext, byteBuffer, workingBuffer, compressionLevel.levelNum)

        return compressedSize.toInt()
    }

    override fun readToChunk(byteBuffer: ByteBuffer, chunk: Chunk) {
        workingBuffer.rewind()
        workingBuffer.limit(workingBuffer.capacity())
        val amountDecompressed = Zstd.ZSTD_decompressDCtx(decompContext, workingBuffer, byteBuffer)
        if (Zstd.ZSTD_isError(amountDecompressed)) {
            error(Zstd.ZSTD_getErrorName(amountDecompressed))
        }
        workingBuffer.limit(amountDecompressed.toInt())
        wrappedMapper.readToChunk(workingBuffer, chunk)
    }

    enum class CompressionLevel(val levelNum: Int) {
        FAST(Zstd.ZSTD_fast),
        DFAST(Zstd.ZSTD_dfast),
        GREEDY(Zstd.ZSTD_greedy),
        LAZY(Zstd.ZSTD_lazy),
        LAZY2(Zstd.ZSTD_lazy2),
        BT_LAZY2(Zstd.ZSTD_btlazy2),
        BT_OPT(Zstd.ZSTD_btopt),
        BT_ULTRA(Zstd.ZSTD_btultra),
        BT_ULTRA2(Zstd.ZSTD_btultra2)
    }
}