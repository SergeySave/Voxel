package com.sergeysav.voxel.common.chunk

/**
 * @author sergeys
 *
 * @constructor Creates a new MutableChunkPosition
 */
class ImmutableChunkPosition(
    override val x: Int = 0,
    override val y: Int = 0,
    override val z: Int = 0
) : ChunkPosition() {

    constructor(chunkPosition: ChunkPosition) : this(chunkPosition.x, chunkPosition.y, chunkPosition.z)
}