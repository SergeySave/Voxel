package com.sergeysav.voxel.common

import com.sergeysav.voxel.client.FrontendProxy
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.block.impl.Dirt
import com.sergeysav.voxel.common.block.impl.Grass
import com.sergeysav.voxel.common.block.impl.Stone
import com.sergeysav.voxel.common.block.impl.Test
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

/**
 * @author sergeys
 */
abstract class CommonProxy : SidedProxy {
    final override val blocks = mutableListOf<Block<*>>()

    init {
        blocks.add(Air)
        blocks.add(Dirt)
        blocks.add(Stone)
        blocks.add(Grass)
        blocks.add(Test)
    }

    override fun initialize(mainThreadRunner: MainThreadRunner) {
    }
}