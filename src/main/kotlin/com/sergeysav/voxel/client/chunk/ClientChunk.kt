package com.sergeysav.voxel.client.chunk

import com.sergeysav.voxel.client.gl.GLDrawingMode
import com.sergeysav.voxel.client.gl.Mesh
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.pool.ConcurrentObjectPool
import com.sergeysav.voxel.common.pool.ObjectPool
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author sergeys
 *
 * @constructor Creates a new ClientChunk
 */
class ClientChunk(position: ChunkPosition) : Chunk(position) {

    var mesh: Mesh? = null
    var isMeshEmpty = true
    var loaded = false
    var adjacentLoadedChunks = AtomicInteger(0)

    override fun reset() {
        isMeshEmpty = true
        loaded = false
        mesh?.let { meshPool.put(it) }
        mesh = null
        adjacentLoadedChunks.set(0)
        super.reset()
    }

    companion object {
        val meshPool: ObjectPool<Mesh> = ConcurrentObjectPool({
            Mesh(GLDrawingMode.TRIANGLES, true)
        }, 1024)
    }
}