package com.sergeysav.voxel.common

import com.sergeysav.voxel.common.block.BlockMesher
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.state.BlockState
import java.util.concurrent.CompletableFuture

/**
 * @author sergeys
 */
interface SidedProxy {

    val blocks: List<Block<*>>

    fun <B : Block<out S>, S : BlockState> getProxyBlockMesher(block: B): BlockMesher<*, B, S>?

    fun <C, B : Block<out S>, S : BlockState> getBlockMesher(block: B): BlockMesher<C, B, S>? {
        @Suppress("UNCHECKED_CAST")
        return getProxyBlockMesher(block) as BlockMesher<C, B, S>?
    }

    fun initialize(mainThreadRunner: MainThreadRunner)

    companion object {
        lateinit var sideProxy: SidedProxy
    }
}
