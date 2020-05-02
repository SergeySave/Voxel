package com.sergeysav.voxel.common.world.generator

import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.impl.Dirt
import com.sergeysav.voxel.common.block.impl.Grass
import com.sergeysav.voxel.common.block.impl.Stone
import com.sergeysav.voxel.common.block.state.DefaultBlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.noise.noiseGenerator
import com.sergeysav.voxel.common.noise.noiseGenerator2d
import com.sergeysav.voxel.common.pool.LocalObjectPool
import com.sergeysav.voxel.common.pool.with
import mu.KotlinLogging
import kotlin.math.floor

/**
 * @author sergeys
 *
 * @constructor Creates a new DevTestGenerator1
 */
class DevTestGenerator1 : ChunkGenerator<Chunk> {

    private val log = KotlinLogging.logger {  }
    private val highFrequencyHeightGenerator = noiseGenerator2d(0, 10f, 1f/20, 8)
    private val lowFrequencyHeightGenerator = noiseGenerator2d(1, 10f, 1f/200, 8)
    private val generatorBlendGenerator = noiseGenerator2d(2, 1f, 1f/200, 2)
    private val blockGenerator = noiseGenerator(3, 10f, 0.05f, 8)
    private val blockPosPool = LocalObjectPool({ MutableBlockPosition() }, 5)

    init {
        log.trace { "Initializing Chunk Generator" }
    }

    override fun generateChunk(chunk: Chunk) {
        blockPosPool.with { blockPos ->
            for (i in 0 until 16) {
                blockPos.x = i
                val x = i + chunk.position.x * Chunk.SIZE
                for (k in 0 until 16) {
                    blockPos.z = k
                    val z = k + chunk.position.z * Chunk.SIZE
                    // blend \in [0, 1]
                    val blend = minOf(maxOf(generatorBlendGenerator(x.toFloat(), z.toFloat()) + 0.5f, 0f), 1f)
                    val height = blend * highFrequencyHeightGenerator(x.toFloat(), z.toFloat()) + (1 - blend) * lowFrequencyHeightGenerator(x.toFloat(), z.toFloat())
                    for (j in 0 until 16) {
                        val y = j + chunk.position.y * 16
                        if (y < height) {
                            blockPos.y = j
                            val block = blockGenerator(x.toFloat(), y.toFloat(), z.toFloat())
                            if (block > 0) {
                                if (floor(height).toInt() == y) {
                                    chunk.setBlock(blockPos, Grass, DefaultBlockState)
                                } else {
                                    chunk.setBlock(blockPos, Dirt, DefaultBlockState)
                                }
                            } else {
                                chunk.setBlock(blockPos, Stone, DefaultBlockState)
                            }
                        }
                    }
                }
            }
        }
    }
}