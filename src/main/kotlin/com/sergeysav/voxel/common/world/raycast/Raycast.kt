package com.sergeysav.voxel.common.world.raycast

import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.data.Direction
import com.sergeysav.voxel.common.pool.LocalObjectPool
import com.sergeysav.voxel.common.pool.with
import com.sergeysav.voxel.common.world.World
import org.joml.Vector3f
import org.joml.Vector3fc
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * @author sergeys
 */
object Raycast {

    private val vector3Pool = LocalObjectPool({ Vector3f() }, 1)
    private val blockPosPool = LocalObjectPool({ MutableBlockPosition() }, 1)

    private fun setFace(raycastResult: RaycastResult, steppedIndex: Int, stepx: Int, stepy: Int, stepz: Int) {
        raycastResult.face = if (steppedIndex == 0) {
            if (stepx == 1) {
                Direction.South
            } else {
                Direction.North
            }
        } else if (steppedIndex == 1) {
            if (stepy == 1) {
                Direction.Down
            } else {
                Direction.Up
            }
        } else { // steppedIndex == 2
            if (stepz == 1) {
                Direction.West
            } else {
                Direction.East
            }
        }
    }

    fun doRaycast(world: World<*>, position: Vector3fc, direction: Vector3fc, maxDistance: Double, raycastResult: RaycastResult) {
        vector3Pool.with { dir ->
            dir.set(direction).normalize()
            blockPosPool.with { blockPos ->
                var t = 0f
                var ix = floor(position.x()).toInt()
                var iy = floor(position.y()).toInt()
                var iz = floor(position.z()).toInt()
                val stepx = if (dir.x() > 0) 1 else -1
                val stepy = if (dir.y() > 0) 1 else -1
                val stepz = if (dir.z() > 0) 1 else -1
                val txDelta = abs(1 / dir.x())
                val tyDelta = abs(1 / dir.y())
                val tzDelta = abs(1 / dir.z())
                val xDist = if (stepx > 0) { ix + 1 - position.x() } else { position.x() - ix }
                val yDist = if (stepy > 0) { iy + 1 - position.y() } else { position.y() - iy }
                val zDist = if (stepz > 0) { iz + 1 - position.z() } else { position.z() - iz }
                var txMax = if (txDelta < Float.POSITIVE_INFINITY) { txDelta * xDist } else { Float.POSITIVE_INFINITY }
                var tyMax = if (tyDelta < Float.POSITIVE_INFINITY) { tyDelta * yDist } else { Float.POSITIVE_INFINITY }
                var tzMax = if (tzDelta < Float.POSITIVE_INFINITY) { tzDelta * zDist } else { Float.POSITIVE_INFINITY }
                var steppedIndex = -1

                // main loop along raycast vector
                while (t <= maxDistance) {
                    // exit check
                    blockPos.x = ix
                    blockPos.y = iy
                    blockPos.z = iz
                    val b = world.getBlock(blockPos)
                    if (b == null) { // Missing Chunk
                        raycastResult.found = false
                        raycastResult.outcome = RaycastResult.RaycastOutcome.CHUNK_NOT_LOADED
                        raycastResult.block = null
                        raycastResult.blockPosition.apply {
                            x = ix
                            y = iy
                            z = iz
                        }
                        setFace(raycastResult, steppedIndex, stepx, stepy, stepz)
                        return
                    } else if (b != Air) { // Block Found
                        raycastResult.found = true
                        raycastResult.outcome = RaycastResult.RaycastOutcome.COMPLETED
                        raycastResult.block = b
                        raycastResult.blockPosition.apply {
                            x = ix
                            y = iy
                            z = iz
                        }
                        setFace(raycastResult, steppedIndex, stepx, stepy, stepz)
                        return
                    } else { // Air (pass through)
                        // advance t to next nearest voxel boundary
                        if (txMax < tyMax) {
                            if (txMax < tzMax) {
                                ix += stepx
                                t = txMax
                                txMax += txDelta
                                steppedIndex = 0
                            } else {
                                iz += stepz
                                t = tzMax
                                tzMax += tzDelta
                                steppedIndex = 2
                            }
                        } else {
                            if (tyMax < tzMax) {
                                iy += stepy
                                t = tyMax
                                tyMax += tyDelta
                                steppedIndex = 1
                            } else {
                                iz += stepz
                                t = tzMax
                                tzMax += tzDelta
                                steppedIndex = 2
                            }
                        }
                    }
                }

                raycastResult.found = false
                raycastResult.outcome = RaycastResult.RaycastOutcome.COMPLETED
                raycastResult.blockPosition.apply {
                    x = ix
                    y = iy
                    z = iz
                }
                raycastResult.block = null
                raycastResult.face = null
            }
        }
    }
}