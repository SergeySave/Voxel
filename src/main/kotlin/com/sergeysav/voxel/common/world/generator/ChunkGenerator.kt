package com.sergeysav.voxel.common.world.generator

import com.sergeysav.voxel.common.chunk.Chunk

/**
 * @author sergeys
 */
interface ChunkGenerator<C : Chunk> {

    fun generateChunk(chunk: C)
}