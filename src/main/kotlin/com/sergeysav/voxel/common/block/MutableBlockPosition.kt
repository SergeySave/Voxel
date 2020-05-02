package com.sergeysav.voxel.common.block

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.math.divisionQuotient
import com.sergeysav.voxel.common.math.divisionRemainder

/**
 * @author sergeys
 *
 * @constructor Creates a new MutableChunkPosition
 */
class MutableBlockPosition(
    override var x: Int = 0,
    override var y: Int = 0,
    override var z: Int = 0
) : BlockPosition() {

    constructor(blockPosition: BlockPosition) : this(blockPosition.x, blockPosition.y, blockPosition.z)

    fun set(other: BlockPosition) {
        this.x = other.x
        this.y = other.y
        this.z = other.z
    }

    fun setToChunkLocal() {
        this.x = this.x.divisionRemainder(Chunk.SIZE)
        this.y = this.y.divisionRemainder(Chunk.SIZE)
        this.z = this.z.divisionRemainder(Chunk.SIZE)
    }
}