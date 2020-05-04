package com.sergeysav.voxel.common.region

/**
 * @author sergeys
 *
 * @constructor Creates a new ImmutableRegionPosition
 */
class ImmutableRegionPosition(
    override val x: Int = 0,
    override val y: Int = 0,
    override val z: Int = 0
) : RegionPosition() {

    constructor(regionPosition: RegionPosition) : this(regionPosition.x, regionPosition.y, regionPosition.z)
}