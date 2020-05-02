package com.sergeysav.voxel.client.chunk

import com.sergeysav.voxel.client.gl.Mesh
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import org.lwjgl.BufferUtils
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author sergeys
 *
 * @constructor Creates a new ClientChunk
 */
class ClientChunk(position: ChunkPosition) : Chunk(position) {

    var mesh: Mesh? = null
    var shouldRender = false
    var adjacentLoadedChunks = AtomicInteger(0)
}