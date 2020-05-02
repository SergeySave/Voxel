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

    abstract fun getAxisTexture(pos: BlockPosition, block: B, state: S, direction: Direction): TextureResource

    abstract fun getAxisRotation(pos: BlockPosition, block: B, state: S, direction: Direction): BlockTextureRotation

    abstract fun getAxisReflection(pos: BlockPosition, block: B, state: S, direction: Direction): BlockTextureReflection

    override fun addToMesh(chunkMesher: ChunkMesherCallback, pos: BlockPosition, block: B, state: S) {
        for (d in Direction.all) {
            chunkMesher.addAAQuad(
                0.0,
                0.0, 1.0, 1.0, 1.0, 1.0,
                0.0, 0.0, 1.0, 1.0, 1.0,
                1.0, 1.0, 1.0, 1.0, 1.0,
                1.0, 0.0, 1.0, 1.0, 1.0,
                getAxisTexture(pos, block, state, d), d, getAxisRotation(pos, block, state, d), getAxisReflection(pos, block, state, d), true
            )
        }
    }
}