package com.sergeysav.voxel.common.chunk

import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.math.divisionQuotient

/**
 * @author sergeys
 *
 * @constructor Creates a new MutableChunkPosition
 */
class MutableChunkPosition(
    override var x: Int = 0,
    override var y: Int = 0,
    override var z: Int = 0
) : ChunkPosition() {

    constructor(chunkPosition: ChunkPosition) : this(chunkPosition.x, chunkPosition.y, chunkPosition.z)

    fun set(other: ChunkPosition) {
        this.x = other.x
        this.y = other.y
        this.z = other.z
    }

    fun setToChunkOf(block: BlockPosition) {
        this.x = block.x.divisionQuotient(Chunk.SIZE)
        this.y = block.y.divisionQuotient(Chunk.SIZE)
        this.z = block.z.divisionQuotient(Chunk.SIZE)
    }
}