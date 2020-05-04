package com.sergeysav.voxel.common.region

import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.math.divisionQuotient

/**
 * @author sergeys
 *
 * @constructor Creates a new MutableRegionPosition
 */
class MutableRegionPosition(
    override var x: Int = 0,
    override var y: Int = 0,
    override var z: Int = 0
) : RegionPosition() {

    constructor(regionPosition: RegionPosition) : this(regionPosition.x, regionPosition.y, regionPosition.z)

    fun set(other: RegionPosition) {
        this.x = other.x
        this.y = other.y
        this.z = other.z
    }

    fun setToRegionOf(chunk: ChunkPosition) {
        this.x = chunk.x.divisionQuotient(Region.SIZE)
        this.y = chunk.y.divisionQuotient(Region.SIZE)
        this.z = chunk.z.divisionQuotient(Region.SIZE)
    }
}