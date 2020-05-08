package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.chunk.meshing.BlockTextureReflection
import com.sergeysav.voxel.client.chunk.meshing.BlockTextureRotation
import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.data.Direction

/**
 * @author sergeys
 *
 * @constructor Creates a new SolidBlockMesher
 */
open class SolidBlockMesher<B : Block<out S>, S : BlockState>(
    val texture: TextureResource
) : CubeBlockMesher<B, S>() {

    override val opaque: Boolean = true
    override val translucent: Boolean = false

    override fun getAxisTexture(pos: BlockPosition, block: B, state: S, direction: Direction): TextureResource = texture

    override fun getAxisRotation(pos: BlockPosition, block: B, state: S, direction: Direction): BlockTextureRotation = BlockTextureRotation.NO_ROTATE

    override fun getAxisReflection(pos: BlockPosition, block: B, state: S, direction: Direction): BlockTextureReflection = BlockTextureReflection.NO_REFLECT
}