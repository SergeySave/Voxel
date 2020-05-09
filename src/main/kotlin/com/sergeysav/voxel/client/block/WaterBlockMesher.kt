package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.chunk.meshing.BlockTextureReflection
import com.sergeysav.voxel.client.chunk.meshing.BlockTextureRotation
import com.sergeysav.voxel.client.chunk.meshing.ChunkMesherCallback
import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.impl.Water
import com.sergeysav.voxel.common.block.state.DefaultBlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.data.Direction
import com.sergeysav.voxel.common.pool.LocalObjectPool
import com.sergeysav.voxel.common.pool.ObjectPool
import com.sergeysav.voxel.common.pool.with
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 *
 * @constructor Creates a new WaterBlockMesher
 */
open class WaterBlockMesher(
    private val texture: TextureResource
) : ClientBlockMesher<Water, DefaultBlockState> {

    override fun shouldOpaqueAdjacentHideFace(side: Direction): Boolean = false

    override fun shouldTransparentAdjacentHideFace(side: Direction): Boolean = true

    private fun fullMesh(mesherCallback: ChunkMesherCallback, direction: Direction, inset: Boolean) {
        mesherCallback.addAAQuad(
            if (inset) 0.3 else 0.0,
            0.0, 1.0, 1.0, 1.0, 1.0,
            0.0, 0.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0, 1.0,
            1.0, 0.0, 1.0, 1.0, 1.0,
            texture, direction, BlockTextureRotation.NO_ROTATE, BlockTextureReflection.NO_REFLECT,
            border = true,
            applyDefaultLighting = false
        )
    }

    private fun partialBottomMesh(mesherCallback: ChunkMesherCallback, direction: Direction) {
        mesherCallback.addAAQuad(
            0.0,
            0.0, 0.7, 1.0, 1.0, 1.0,
            0.0, 0.0, 1.0, 1.0, 1.0,
            1.0, 0.7, 1.0, 1.0, 1.0,
            1.0, 0.0, 1.0, 1.0, 1.0,
            texture, direction, BlockTextureRotation.NO_ROTATE, BlockTextureReflection.NO_REFLECT,
            border = true,
            applyDefaultLighting = false
        )
    }

    private fun partialTopMesh(mesherCallback: ChunkMesherCallback, direction: Direction) {
        mesherCallback.addAAQuad(
            0.0,
            0.0, 1.0, 1.0, 1.0, 1.0,
            0.0, 0.7, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0, 1.0,
            1.0, 0.7, 1.0, 1.0, 1.0,
            texture, direction, BlockTextureRotation.NO_ROTATE, BlockTextureReflection.NO_REFLECT,
            border = false,
            applyDefaultLighting = false
        )
    }

    override fun addToMesh(
        opaqueMesher: ChunkMesherCallback,
        translucentMesher: ChunkMesherCallback,
        pos: BlockPosition,
        block: Water,
        state: DefaultBlockState,
        world: World<Chunk>
    ) {
        fullMesh(translucentMesher, Direction.Down, false)
        blockPosPool.with { blockPos ->
            blockPos.set(pos)
            blockPos.apply {
                x += Direction.Up.relX
                y += Direction.Up.relY
                z += Direction.Up.relZ
            }
            val waterAbove = world.getBlock(blockPos) == Water

            for (i in Direction.flat.indices) {
                val d = Direction.flat[i]
                blockPos.set(pos)
                blockPos.apply {
                    x += d.relX
                    y += d.relY
                    z += d.relZ
                }
                val waterDirection = world.getBlock(blockPos) == Water

                if (waterAbove) {
                    if (waterDirection) {
                        blockPos.apply {
                            x += Direction.Up.relX
                            y += Direction.Up.relY
                            z += Direction.Up.relZ
                        }
                        val waterAboveAdjacent = world.getBlock(blockPos) == Water
                        if (!waterAboveAdjacent) {
                            partialTopMesh(translucentMesher, d)
                        }
                    } else {
                        fullMesh(translucentMesher, d, false)
                    }
                } else {
                    if (!waterDirection) {
                        partialBottomMesh(translucentMesher, d)
                    }
                }
            }

            if (!waterAbove) {
                fullMesh(translucentMesher, Direction.Up, true)
            }
        }

    }

    companion object {
        private val blockPosPool: ObjectPool<MutableBlockPosition> = LocalObjectPool({ MutableBlockPosition() }, 2)
    }
}