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
        strategies.forEach(WorldLoadingStrategy::update)

        var positions = chunks.map(Chunk::position)
        strategies.forEach {
            positions = positions.filterNot(it::shouldStayLoaded)
        }
        positions.forEach(unloadCallback)

        strategies.forEach {
            it.requestLoads(world, chunks, loadCallback)
        }
    }

    override fun cleanupWorldLoading() {
        strategies.forEach(WorldLoadingStrategy::cleanup)
    }
}