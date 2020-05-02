package com.sergeysav.voxel.common.world.raycast

import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.data.Direction

/**
 * @author sergeys
 *
 * @constructor Creates a new RaycastResult
 */
data class RaycastResult(
    var found: Boolean,
    var outcome: RaycastOutcome,
    var blockPosition: MutableBlockPosition,
    var block: Block<*>?,
    var face: Direction?
) {

    enum class RaycastOutcome {
        COMPLETED,
        CHUNK_NOT_LOADED
    }
}