package com.sergeysav.voxel.common.world.generator

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 */
interface ChunkGenerator<C : Chunk> {

    fun generateChunk(chunk: C, world: World<C>)
}