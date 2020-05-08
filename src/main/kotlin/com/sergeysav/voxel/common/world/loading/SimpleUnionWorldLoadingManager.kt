package com.sergeysav.voxel.common.world.loading

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.world.World
import mu.KotlinLogging

/**
 * @author sergeys
 *
 * @constructor Creates a new UnionWorldLoadingManager
 */
class SimpleUnionWorldLoadingManager(vararg strategies: WorldLoadingStrategy) : WorldLoadingManager<Chunk, World<Chunk>> {

    private val log = KotlinLogging.logger {  }
    private val strategies = mutableListOf<WorldLoadingStrategy>()
    private val toUnload = ArrayList<ChunkPosition>(1000)

    init {
        log.trace { "Initializing World Loading Manager" }
        for (i in strategies.indices) {
            this.strategies.add(strategies[i])
        }
    }

    override fun updateWorldLoading(
        world: World<Chunk>,
        chunks: List<Chunk>,
        loadCallback: (ChunkPosition) -> Unit,
        unloadCallback: (ChunkPosition) -> Unit
    ) {
        for (i in strategies.indices) {
            strategies[i].update()
        }

        toUnload.clear()
        ChunkLoop@for (i in chunks.indices) {
            val chunk = chunks[i]
            for (j in strategies.indices) {
                if (strategies[j].shouldStayLoaded(chunk.position)) continue@ChunkLoop
            }
            toUnload.add(chunk.position)
        }
        for (i in toUnload.indices) {
            val position = toUnload[i]
            unloadCallback(position)
        }
        toUnload.clear()

        for (i in strategies.indices) {
            strategies[i].requestLoads(world, chunks, loadCallback)
        }
    }

    override fun cleanupWorldLoading() {
        for (i in strategies.indices) {
            strategies[i].cleanup()
        }
    }
}