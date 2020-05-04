package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.chunk.meshing.BlockTextureReflection
import com.sergeysav.voxel.client.chunk.meshing.BlockTextureRotation
import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.data.Direction
import com.sergeysav.voxel.common.pool.ConcurrentObjectPool
import com.sergeysav.voxel.common.pool.LocalObjectPool
import com.sergeysav.voxel.common.pool.with
import java.util.Random

/**
 * @author sergeys
 *
 * @constructor Creates a new SolidBlockMesher
 */
open class RandomizedSolidBlockMesher<B : Block<out S>, S : BlockState>(
    val texture: TextureResource
) : CubeBlockMesher<B, S>() {

    override val opaque: Boolean = true

    private fun getSeed(pos: BlockPosition, block: B, state: S, direction: Direction, salt: Long) = (((pos.hashCode() * 31L + block.hashCode()) * 31L + state.hashCode()) * 31L + direction.hashCode()) * 31L + salt

    protected fun random(pos: BlockPosition, block: B, state: S, direction: Direction, salt: Long) = randomPool.with {
        it.setSeed(getSeed(pos, block, state, direction, salt))
        it.nextDouble()
    }

    override fun getAxisTexture(pos: BlockPosition, block: B, state: S, direction: Direction): TextureResource = texture

    override fun getAxisRotation(pos: BlockPosition, block: B, state: S, direction: Direction): BlockTextureRotation
            = BlockTextureRotation.all[(random(pos, block, state, direction, 0L) * BlockTextureRotation.all.size).toInt()]

    override fun getAxisReflection(pos: BlockPosition, block: B, state: S, direction: Direction): BlockTextureReflection
            = BlockTextureReflection.all[(random(pos, block, state, direction, 1L) * BlockTextureReflection.all.size).toInt()]

    companion object {
        val randomPool = LocalObjectPool({ Random() }, 1)
    }
}