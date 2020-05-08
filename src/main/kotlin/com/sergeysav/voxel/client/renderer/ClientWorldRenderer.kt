package com.sergeysav.voxel.client.renderer

import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.client.chunk.ClientChunk

/**
 * @author sergeys
 */
interface ClientWorldRenderer {

    fun initialize()

    fun render(camera: Camera, chunks: List<ClientChunk>, width: Int, height: Int)

    fun cleanup()
}