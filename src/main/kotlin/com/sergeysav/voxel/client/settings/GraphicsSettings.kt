package com.sergeysav.voxel.client.settings

/**
 * @author sergeys
 *
 * @constructor Creates a new GraphicsSettings
 */
data class GraphicsSettings(
    val fov: Double = 45.0,
    val meshingSettings: MeshingSettings = MeshingSettings(),
    val chunkManagerSettings: ChunkManagerSettings = ChunkManagerSettings()
)

data class MeshingSettings(
    val parallelism: Int = 8,
    val meshesPerFrame: Int = 64,
    val internalQueueSize: Int = 2048
)

data class ChunkManagerSettings(
    val regionFilesBasePath: String = "world",
    val internalQueueSize: Int = 1024,
    val loadingParallelism: Int = 8,
    val savingParallelism: Int = 8
)
