package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.chunk.meshing.BlockTextureReflection
import com.sergeysav.voxel.client.chunk.meshing.BlockTextureRotation
import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.AxialBlockState
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.data.Direction
import kotlin.random.Random

/**
 * @author sergeys
 *
 * @constructor Creates a new AxialSolidBlockMesher
 */
class GrassBlockMesher<B : Block<out S>, S : BlockState>(
    private val up: TextureResource,
    private val down: TextureResource,
    side: TextureResource
) : RandomizedSolidBlockMesher<B, S>(side) {

    override fun getAxisRotation(pos: BlockPosition, block: B, state: S, direction: Direction) = when (direction) {
        Direction.Up -> super.getAxisRotation(pos, block, state, direction)
        Direction.Down -> super.getAxisRotation(pos, block, state, direction)
        else -> BlockTextureRotation.NO_ROTATE
    }

    override fun getAxisReflection(pos: BlockPosition, block: B, state: S, direction: Direction) = when (direction) {
        Direction.Up -> super.getAxisReflection(pos, block, state, direction)
        Direction.Down -> super.getAxisReflection(pos, block, state, direction)
        else -> if(Random(getSeed(pos, block, state, direction, 2)).nextBoolean()) {
            BlockTextureReflection.NO_REFLECT
        } else {
            BlockTextureReflection.X_REFLECT
        }
    }

    override fun getAxisTexture(pos: BlockPosition, block: B, state: S, direction: Direction): TextureResource = when (direction) {
        Direction.Up -> up
        Direction.Down -> down
        else -> super.getAxisTexture(pos, block, state, direction)
    }
}