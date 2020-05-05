package com.sergeysav.voxel.common.chunk.datamapper

import com.sergeysav.voxel.common.Voxel
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.chunk.Chunk
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * @author sergeys
 *
 * @constructor Creates a new NaiveChunkDataMapper
 */
class NaiveChunkDataMapper : ChunkDataMapper {
    private val blockPos = MutableBlockPosition()
    private val utf8 = Charset.forName("UTF-8")

    private val block = Array<Block<*>>(1) { Air }

    override fun writeFromChunk(byteBuffer: ByteBuffer, chunk: Chunk): Int {
        byteBuffer.put(if (chunk.generated) 1.toByte() else 0.toByte())
        for (x in 0 until Chunk.SIZE) {
            blockPos.x = x
            for (y in 0 until Chunk.SIZE) {
                blockPos.y = y
                for (z in 0 until Chunk.SIZE) {
                    blockPos.z = z

                    val blockState = chunk.getBlockAndState(blockPos, block)
                    @Suppress("UNCHECKED_CAST")
                    val block = block[0] as Block<BlockState>
                    val unlocalizedName = block.unlocalizedName
                    val simpleValue = block.getSimpleValueForState(blockState)
                    byteBuffer.putInt(unlocalizedName.length)

                    byteBuffer.put(unlocalizedName.toByteArray(utf8))
                    byteBuffer.put(simpleValue)
                }
            }
        }
        return byteBuffer.position()
    }

    override fun readToChunk(byteBuffer: ByteBuffer, chunk: Chunk) {
        chunk.generated = byteBuffer.get() != 0.toByte()
        for (x in 0 until Chunk.SIZE) {
            blockPos.x = x
            for (y in 0 until Chunk.SIZE) {
                blockPos.y = y
                for (z in 0 until Chunk.SIZE) {
                    blockPos.z = z

                    val unlocalizedNameLength = byteBuffer.int
                    val unlocalizedName = String(ByteArray(unlocalizedNameLength) { byteBuffer.get() }, utf8)
                    val simpleValue = byteBuffer.get()

                    @Suppress("UNCHECKED_CAST")
                    val block = Voxel.blocks.firstOrNull { it.unlocalizedName == unlocalizedName } as Block<BlockState>? ?: error("Block Not Found")
                    val state = block.getStateFromSimpleValue(simpleValue)

                    chunk.setBlock(blockPos, block, state)
                }
            }
        }
    }
}