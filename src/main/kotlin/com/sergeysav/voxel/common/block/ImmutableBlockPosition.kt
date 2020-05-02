package com.sergeysav.voxel.common.block

/**
 * @author sergeys
 *
 * @constructor Creates a new ImmutableChunkPosition
 */
class ImmutableBlockPosition(
    override val x: Int = 0,
    override val y: Int = 0,
    override val z: Int = 0
) : BlockPosition() {

    constructor(blockPosition: BlockPosition) : this(blockPosition.x, blockPosition.y, blockPosition.z)
}
