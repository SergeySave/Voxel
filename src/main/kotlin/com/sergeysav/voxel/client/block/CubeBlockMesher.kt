package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.chunk.meshing.BlockTextureReflection
import com.sergeysav.voxel.client.chunk.meshing.BlockTextureRotation
import com.sergeysav.voxel.client.chunk.meshing.ChunkMesherCallback
import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.data.Direction

/**
 * @author sergeys
 */
abstract class CubeBlockMesher<B : Block<out S>, S : BlockState> : ClientBlockMesher<B, S> {

    protected abstract val translucent: Boolean
    protected abstract val opaque: Boolean
    protected open val doubleRender: Boolean = true

    abstract fun getAxisTexture(pos: BlockPosition, block: B, state: S, direction: Direction): TextureResource

    abstract fun getAxisRotation(pos: BlockPosition, block: B, state: S, direction: Direction): BlockTextureRotation

    abstract fun getAxisReflection(pos: BlockPosition, block: B, state: S, direction: Direction): BlockTextureReflection

    override fun addToMesh(opaqueMesher: ChunkMesherCallback, translucentMesher: ChunkMesherCallback, pos: BlockPosition, block: B, state: S) {
        val mesher = if (translucent) translucentMesher else opaqueMesher
        for (i in Direction.all.indices) {
            val d = Direction.all[i]
            mesher.addAAQuad(
                0.0,
                0.0, 1.0, 1.0, 1.0, 1.0,
                0.0, 0.0, 1.0, 1.0, 1.0,
                1.0, 1.0, 1.0, 1.0, 1.0,
                1.0, 0.0, 1.0, 1.0, 1.0,
                getAxisTexture(pos, block, state, d), d, getAxisRotation(pos, block, state, d), getAxisReflection(pos, block, state, d),
                border = true,
                doubleRender = doubleRender
            )
        }
    }

    override fun shouldOpaqueAdjacentHideFace(side: Direction): Boolean = opaque
    override fun shouldTransparentAdjacentHideFace(side: Direction): Boolean = translucent
}