package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.chunk.meshing.BlockTextureReflection
import com.sergeysav.voxel.client.chunk.meshing.BlockTextureRotation
import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.data.Direction
import kotlin.random.Random

/**
 * @author sergeys
 *
 * @constructor Creates a new SolidBlockMesher
 */
open class RandomizedSolidBlockMesher<B : Block<out S>, S : BlockState>(
    val texture: TextureResource
) : CubeBlockMesher<B, S>() {

    override val opaque: Boolean = true

    protected fun getSeed(pos: BlockPosition, block: B, state: S, direction: Direction, salt: Int) = (((pos.hashCode() * 31 + block.hashCode()) * 31 + state.hashCode()) * 31 + direction.hashCode()) * 31 + salt

    override fun getAxisTexture(pos: BlockPosition, block: B, state: S, direction: Direction): TextureResource = texture

    override fun getAxisRotation(pos: BlockPosition, block: B, state: S, direction: Direction): BlockTextureRotation
            = BlockTextureRotation.values().random(Random(getSeed(pos, block, state, direction, 0)))

    override fun getAxisReflection(pos: BlockPosition, block: B, state: S, direction: Direction): BlockTextureReflection
            = BlockTextureReflection.values().random(Random(getSeed(pos, block, state, direction, 1)))
}