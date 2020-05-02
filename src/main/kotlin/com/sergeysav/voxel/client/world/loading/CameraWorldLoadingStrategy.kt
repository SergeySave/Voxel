package com.sergeysav.voxel.client.world.loading

import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.client.camera.CameraController
import com.sergeysav.voxel.client.camera.CameraAABBChecker
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.MutableChunkPosition
import com.sergeysav.voxel.common.math.divisionQuotient
import com.sergeysav.voxel.common.math.square
import com.sergeysav.voxel.common.world.World
import com.sergeysav.voxel.common.world.loading.WorldLoadingStrategy
import kotlin.math.roundToInt

/**
 * @author sergeys
 *
 * @constructor Creates a new CameraWorldLoadingStrategy
 */
class CameraWorldLoadingStrategy(private val camera: Camera, var distance: Int) : WorldLoadingStrategy {

    private val chunkPosition = MutableChunkPosition()
    private val cameraAABBChecker = CameraAABBChecker()

    override fun update() {
        cameraAABBChecker.update(camera)
    }

    override fun shouldStayLoaded(chunkPosition: ChunkPosition): Boolean {
        return ((chunkPosition.x + 0.5) * Chunk.SIZE - camera.position.x()).square() +
                ((chunkPosition.y + 0.5) * Chunk.SIZE - camera.position.y()).square() +
                ((chunkPosition.z + 0.5) * Chunk.SIZE - camera.position.z()).square() <= (distance * Chunk.SIZE).square()
    }

    override fun requestLoads(world: World<Chunk>, chunks: Iterable<Chunk>, loadCallback: (ChunkPosition) -> Unit) {
        for (i in -distance..distance) {
            for (j in -distance..distance) {
                for (k in -distance..distance) {
                    if (i.square() + j.square() + k.square() <= distance.square()) {
                        chunkPosition.y = camera.position.y().roundToInt().divisionQuotient(Chunk.SIZE) + i
                        chunkPosition.x = camera.position.x().roundToInt().divisionQuotient(Chunk.SIZE) + j
                        chunkPosition.z = camera.position.z().roundToInt().divisionQuotient(Chunk.SIZE) + k
                        if (cameraAABBChecker.isAABBinCamera(
                                chunkPosition.x * Chunk.SIZE.toFloat(),
                                chunkPosition.y * Chunk.SIZE.toFloat(),
                                chunkPosition.z * Chunk.SIZE.toFloat(),
                                Chunk.SIZE.toFloat(),
                                Chunk.SIZE.toFloat(),
                                Chunk.SIZE.toFloat())) {
                            loadCallback(chunkPosition)
                        }
                    }
                }
            }
        }
    }

    override fun cleanup() {
    }
}