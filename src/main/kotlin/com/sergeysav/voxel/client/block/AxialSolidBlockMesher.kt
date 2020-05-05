package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.AxialBlockState
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.data.Direction

/**
 * @author sergeys
 *
 * @constructor Creates a new AxialSolidBlockMesher
 */
class AxialSolidBlockMesher<B : Block<out S>, S : AxialBlockState>(
    sideTexture: TextureResource,
    private val facingTexture: TextureResource,
    private val backTexture: TextureResource = facingTexture
) : SolidBlockMesher<B, S>(sideTexture) {

    override fun getAxisTexture(pos: BlockPosition, block: B, state: S, direction: Direction): TextureResource = when (direction) {
        state.axis -> facingTexture
        state.axis.opposite -> backTexture
        else -> super.getAxisTexture(pos, block, state, direction)
    }
}