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
        for (s in strategies) {
            this.strategies.add(s)
        }
    }

    override fun updateWorldLoading(
        world: World<Chunk>,
        chunks: Iterable<Chunk>,
        loadCallback: (ChunkPosition) -> Unit,
        unloadCallback: (ChunkPosition) -> Unit
    ) {
        for (strategy in strategies) {
            strategy.update()
        }

        toUnload.clear()
        ChunkLoop@for (chunk in chunks) {
            for (strategy in strategies) {
                if (strategy.shouldStayLoaded(chunk.position)) continue@ChunkLoop
            }
            toUnload.add(chunk.position)
        }
        for (position in toUnload) {
            unloadCallback(position)
        }
        toUnload.clear()

        for (strategy in strategies) {
            strategy.requestLoads(world, chunks, loadCallback)
        }
    }

    override fun cleanupWorldLoading() {
        for (strategy in strategies) {
            strategy.cleanup()
        }
    }
}