package com.sergeysav.voxel.common.chunk.datamapper

import com.sergeysav.voxel.common.chunk.Chunk
import java.nio.ByteBuffer

/**
 * @author sergeys
 */
interface ChunkDataMapper {
    fun writeFromChunk(byteBuffer: ByteBuffer, chunk: Chunk): Int
    fun readToChunk(byteBuffer: ByteBuffer, chunk: Chunk)
}