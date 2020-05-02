package com.sergeysav.voxel.client.world

import com.sergeysav.voxel.client.FrontendProxy
import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.client.camera.CameraAABBChecker
import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.client.gl.bound
import com.sergeysav.voxel.client.gl.setUniform
import com.sergeysav.voxel.client.player.PlayerInput
import com.sergeysav.voxel.client.world.meshing.WorldMeshingManager
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.block.impl.Test
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.block.state.DefaultBlockState
import com.sergeysav.voxel.common.bound
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.ImmutableChunkPosition
import com.sergeysav.voxel.common.chunk.MutableChunkPosition
import com.sergeysav.voxel.common.data.Direction
import com.sergeysav.voxel.common.pool.LocalObjectPool
import com.sergeysav.voxel.common.pool.with
import com.sergeysav.voxel.common.world.World
import com.sergeysav.voxel.common.world.chunks.ChunkManager
import com.sergeysav.voxel.common.world.loading.WorldLoadingManager
import com.sergeysav.voxel.common.world.raycast.Raycast
import com.sergeysav.voxel.common.world.raycast.RaycastResult
import mu.KotlinLogging
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20

/**
 * @author sergeys
 *
 * @constructor Creates a new ClientWorld
 */
class ClientWorld(
    private val worldLoadingManager: WorldLoadingManager<in ClientChunk, in ClientWorld>,
    private val worldMeshingManager: WorldMeshingManager<ClientWorld>,
    private val chunkManager: ChunkManager<ClientChunk>
) : World<ClientChunk> {

    private val model = Matrix4f()
    private val log = KotlinLogging.logger {  }
    private val blockPosPool = LocalObjectPool({ MutableBlockPosition() }, 5)
    private val chunkPosPool = LocalObjectPool({ MutableChunkPosition() }, 5)
    private val raycastResultPool = LocalObjectPool({ RaycastResult(false, RaycastResult.RaycastOutcome.COMPLETED, MutableBlockPosition(), null, null) }, 1)
    private val chunks = mutableMapOf<ChunkPosition, ClientChunk>()
    private val cameraAABBChecker = CameraAABBChecker()

    init {
        log.info { "Initializing Client World" }
        chunkManager.initialize { clientChunk ->
            clientChunk.shouldRender = true
            worldMeshingManager.notifyMeshDirty(clientChunk)

            chunkPosPool.with {
                for (d in Direction.all) {
                    it.set(clientChunk.position)
                    it.x += d.relX
                    it.y += d.relY
                    it.z += d.relZ
                    chunks[it]?.let { c -> c.adjacentLoadedChunks.incrementAndGet(); worldMeshingManager.notifyMeshDirty(c) }
                }
            }
        }
    }

    private fun doLoad(chunkPosition: ChunkPosition) {
        if (!chunks.containsKey(chunkPosition)) {
            val chunk = ClientChunk(ImmutableChunkPosition(chunkPosition))

            chunks[chunk.position] = chunk
            chunkManager.requestLoad(chunk)
        }
    }

    private fun doUnload(chunkPosition: ChunkPosition) {
        chunks.remove(chunkPosition)?.let {
            worldMeshingManager.notifyMeshUnneeded(it)
            chunkManager.requestUnload(it)
        }

        chunkPosPool.with {
            for (d in Direction.all) {
                it.set(chunkPosition)
                it.x += d.relX
                it.y += d.relY
                it.z += d.relZ
                chunks[it]?.let { c -> c.adjacentLoadedChunks.decrementAndGet(); worldMeshingManager.notifyMeshDirty(c) }
            }
        }
    }

    override fun <T : BlockState> setBlock(blockPosition: BlockPosition, block: Block<T>, blockState: T) {
        chunkPosPool.with { chunkPos ->
            chunkPos.setToChunkOf(blockPosition)
            blockPosPool.with { blockPos ->
                blockPos.set(blockPosition)
                blockPos.setToChunkLocal()
                chunks[chunkPos]?.let {
                    it.setBlock(blockPos, block, blockState)
                    worldMeshingManager.notifyMeshDirty(it)
                }
                chunkPosPool.with {
                    for (d in Direction.all) {
                        it.set(chunkPos)
                        it.x += d.relX
                        it.y += d.relY
                        it.z += d.relZ
                        chunks[it]?.let { c -> worldMeshingManager.notifyMeshDirty(c) }
                    }
                }
            }
        }
    }

    override fun getBlock(blockPosition: BlockPosition): Block<*>? {
        return chunkPosPool.with { chunkPos ->
            chunkPos.setToChunkOf(blockPosition)
            blockPosPool.with { blockPos ->
                blockPos.set(blockPosition)
                blockPos.setToChunkLocal()
                chunks[chunkPos]?.getBlock(blockPos)
            }
        }
    }

    override fun getBlockState(blockPosition: BlockPosition): BlockState? {
        return chunkPosPool.with { chunkPos ->
            chunkPos.setToChunkOf(blockPosition)
            blockPosPool.with { blockPos ->
                blockPos.set(blockPosition)
                blockPos.setToChunkLocal()
                chunks[chunkPos]?.getBlockState(blockPos)
            }
        }
    }

    override fun update() {
        chunkManager.update()
        worldLoadingManager.updateWorldLoading(this, this.chunks.values, this::doLoad, this::doUnload)
        worldMeshingManager.updateWorldMeshing(this)
    }

    fun draw(camera: Camera, playerInput: PlayerInput) {
        if (playerInput.mouseButton1JustUp) {
            raycastResultPool.with { res ->
                Raycast.doRaycast(this, camera.position, camera.direction, 64.0, res)
                if (res.found) {
                    setBlock(res.blockPosition, Air, DefaultBlockState)
                }
            }
        }
        if (playerInput.mouseButton2JustUp) {
            raycastResultPool.with { res ->
                Raycast.doRaycast(this, camera.position, camera.direction, 64.0, res)
                if (res.found) {
                    res.blockPosition.apply {
                        x += (res.face?.relX ?: 0)
                        y += (res.face?.relY ?: 0)
                        z += (res.face?.relZ ?: 0)
                    }
                    setBlock(res.blockPosition, Test, Test.states.first { it.axis == res.face })
                }
            }
        }
        cameraAABBChecker.update(camera)
        FrontendProxy.voxelShader.bound {
            camera.combined.setUniform(FrontendProxy.voxelShader.getUniform("uCamera"))
            FrontendProxy.assetData.bound(0) {
                GL20.glUniform1i(FrontendProxy.voxelShader.getUniform("assetData"), 0)
                FrontendProxy.textureAtlas.bound(1) {
                    GL20.glUniform1i(FrontendProxy.voxelShader.getUniform("atlasPage0"), 1)
                    chunks.values.asSequence()
                        .filter { it.shouldRender }
                        .filter { it.mesh != null }
                        .sortedBy {  // Sort chunks in based on the direction that the camera is facing (dot product)
                            (it.position.x + 0.5) * Chunk.SIZE * camera.direction.x() +
                                    (it.position.y + 0.5) * Chunk.SIZE * camera.direction.y() +
                                    (it.position.z + 0.5) * Chunk.SIZE * camera.direction.z()
                        }.forEach { c ->
                            val mesh = c.mesh
                            val x = c.position.x * Chunk.SIZE.toFloat()
                            val y = c.position.y * Chunk.SIZE.toFloat()
                            val z = c.position.z * Chunk.SIZE.toFloat()
                            if (mesh != null && cameraAABBChecker.isAABBinCamera(x, y, z, 16f, 16f, 16f)) {
                                model.identity()
                                model.translate(x, y, z)
                                model.setUniform(FrontendProxy.voxelShader.getUniform("uModel"))
                                mesh.draw()
                            }
                        }
                }
            }
        }
    }

    override fun cleanup() {
        worldLoadingManager.cleanupWorldLoading()
        worldMeshingManager.cleanupWorldMeshing()
        for (c in chunks.values) {
            c.mesh?.cleanup()
        }
        blockPosPool.cleanup()
        chunkPosPool.cleanup()
        chunkManager.cleanup()
    }
}