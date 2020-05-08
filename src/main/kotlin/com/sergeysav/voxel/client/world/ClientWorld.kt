package com.sergeysav.voxel.client.world

import com.sergeysav.voxel.client.FrontendProxy
import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.client.camera.CameraAABBChecker
import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.client.gl.bound
import com.sergeysav.voxel.client.gl.setUniform
import com.sergeysav.voxel.client.player.PlayerInput
import com.sergeysav.voxel.client.renderer.ClientWorldRenderer
import com.sergeysav.voxel.client.world.meshing.WorldMeshingManager
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.block.impl.Test
import com.sergeysav.voxel.common.block.impl.Water
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.block.state.DefaultBlockState
import com.sergeysav.voxel.common.bound
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.MutableChunkPosition
import com.sergeysav.voxel.common.data.Direction
import com.sergeysav.voxel.common.pool.LocalObjectPool
import com.sergeysav.voxel.common.pool.ObjectPool
import com.sergeysav.voxel.common.pool.SynchronizedObjectPool
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
    private val chunkManager: ChunkManager<ClientChunk>,
    private val clientWorldRenderer: ClientWorldRenderer
) : World<ClientChunk> {

    private val log = KotlinLogging.logger {  }
    private val blockPosPool = LocalObjectPool({ MutableBlockPosition() }, 5)
    private val chunkPosPool = LocalObjectPool({ MutableChunkPosition() }, 5)
    private val raycastResultPool = LocalObjectPool({ RaycastResult(false, RaycastResult.RaycastOutcome.COMPLETED, MutableBlockPosition(), null, null) }, 1)
    private val chunks = HashMap<ChunkPosition, ClientChunk>()
    private val chunkList = ArrayList<ClientChunk>(4096)
    private val chunkPool: ObjectPool<ClientChunk> = SynchronizedObjectPool({ ClientChunk(MutableChunkPosition()) }, 256)

    init {
        log.info { "Initializing Client World" }
        chunkManager.initialize(this, this::releaseChunk) { clientChunk ->
            clientChunk.loaded = true
            worldMeshingManager.notifyMeshDirty(clientChunk)

            chunkPosPool.with {
                for (d in Direction.all) {
                    it.set(clientChunk.position)
                    it.x += d.relX
                    it.y += d.relY
                    it.z += d.relZ
                    chunks[it]?.let { c ->
                        if (c.loaded) { // If the adjacent chunk is loaded set our adjacent chunks correctly
                            clientChunk.adjacentLoadedChunks.incrementAndGet();
                        }
                        c.adjacentLoadedChunks.incrementAndGet();
                        worldMeshingManager.notifyMeshDirty(c)
                    }
                }
            }
        }

        clientWorldRenderer.initialize()
    }

    private fun getNewChunk(chunkPosition: ChunkPosition): ClientChunk {
        val (x, y, z) = chunkPosition
        val chunk = chunkPool.get()
        (chunk.position as MutableChunkPosition).apply {
            this.x = x
            this.y = y
            this.z = z
        }
        return chunk
    }

    private fun releaseChunk(chunk: ClientChunk) {
        chunk.reset()
        chunkPool.put(chunk)
    }

    private fun doLoad(chunkPosition: ChunkPosition) {
        if (!chunks.containsKey(chunkPosition)) {
            val chunk = getNewChunk(chunkPosition)
            chunks[chunk.position] = chunk
            chunkList.add(chunk)
            chunkManager.requestLoad(chunk)
        }
    }

    private fun doUnload(chunkPosition: ChunkPosition) {
        chunks.remove(chunkPosition)?.let {
            chunkList.remove(it)
            worldMeshingManager.notifyMeshUnneeded(it)

            chunkPosPool.with { pos ->
                for (d in Direction.all) {
                    pos.set(chunkPosition)
                    pos.x += d.relX
                    pos.y += d.relY
                    pos.z += d.relZ
                    chunks[pos]?.let { c ->
                        if (it.loaded) {
                            c.adjacentLoadedChunks.decrementAndGet()
                        }
                        worldMeshingManager.notifyMeshDirty(c)
                    }
                }
            }

            chunkManager.requestUnload(it)
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
                    chunkManager.notifyChunkDirty(it)
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

    override fun <T : BlockState> setBlockOrMeta(blockPosition: BlockPosition, block: Block<T>, blockState: T) {
        chunkPosPool.with { chunkPos ->
            chunkPos.setToChunkOf(blockPosition)
            blockPosPool.with { blockPos ->
                blockPos.set(blockPosition)
                blockPos.setToChunkLocal()
                val chunk = chunks[chunkPos]
                if (chunk != null) {
                    chunk.setBlock(blockPos, block, blockState)
                    if (chunk.loaded) {
                        worldMeshingManager.notifyMeshDirty(chunk)
                        chunkManager.notifyChunkDirty(chunk)

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
                } else {
                    chunkManager.setUnloadedChunkBlock(chunkPos, blockPos, block, blockState)
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
        worldLoadingManager.updateWorldLoading(this, this.chunkList, this::doLoad, this::doUnload)
        worldMeshingManager.updateWorldMeshing(this)
    }

    fun draw(camera: Camera, playerInput: PlayerInput, width: Int, height: Int) {
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

        clientWorldRenderer.render(camera, chunkList, width, height)
    }

    override fun cleanup() {
        worldLoadingManager.cleanupWorldLoading()
        worldMeshingManager.cleanupWorldMeshing()
        for (i in chunkList.indices) {
            chunkList[i].opaqueMesh?.cleanup()
        }
        blockPosPool.cleanup()
        chunkPosPool.cleanup()
        chunkManager.cleanup()
        clientWorldRenderer.cleanup()
    }
}