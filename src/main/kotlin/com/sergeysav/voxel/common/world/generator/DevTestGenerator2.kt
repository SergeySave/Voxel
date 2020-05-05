package com.sergeysav.voxel.common.world.generator

import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.block.impl.Dirt
import com.sergeysav.voxel.common.block.impl.Grass
import com.sergeysav.voxel.common.block.impl.Leaves
import com.sergeysav.voxel.common.block.impl.Log
import com.sergeysav.voxel.common.block.impl.Stone
import com.sergeysav.voxel.common.block.state.AxialBlockState
import com.sergeysav.voxel.common.block.state.DefaultBlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.noise.noiseGenerator2d
import com.sergeysav.voxel.common.pool.LocalObjectPool
import com.sergeysav.voxel.common.pool.with
import com.sergeysav.voxel.common.world.World
import mu.KotlinLogging
import java.util.Random
import kotlin.math.floor

/**
 * @author sergeys
 *
 * @constructor Creates a new DevTestGenerator1
 */
class DevTestGenerator2(private val seed: Long) : ChunkGenerator<Chunk> {

    private val log = KotlinLogging.logger {  }
    private val highFrequencyHeightGenerator = noiseGenerator2d(seed, 10f, 1f/20, 8)
    private val lowFrequencyHeightGenerator = noiseGenerator2d(seed + 1, 10f, 1f/200, 8)
    private val generatorBlendGenerator = noiseGenerator2d(seed + 2, 1f, 1f/200, 2)
    private val dirtDepthGenerator = noiseGenerator2d(seed + 3, 1.5f, 1f/50, 1)
    private val blockPosPool = LocalObjectPool({ MutableBlockPosition() }, 1)
    private val randomPool = LocalObjectPool({ Random() }, 1)
    private val treeGenPositions = Array(5) { MutableBlockPosition() }

    init {
        log.trace { "Initializing Chunk Generator" }
    }

    private fun generateTree(world: World<Chunk>, basePosition: MutableBlockPosition) {
        fun gen4LeavesLayer() {
            basePosition.x++
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.x -= 2
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.x++
            basePosition.z++
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.z -= 2
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.z++
        }
        fun gen8LeavesLayer() {
            gen4LeavesLayer()
            basePosition.x++
            basePosition.z++
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.x -= 2
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.z -= 2
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.x += 2
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.z++
            basePosition.x--
        }
        fun gen17LeavesLayer() {
            gen8LeavesLayer()

            basePosition.x+=2
            basePosition.z++
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.z--
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.z--
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.z++
            basePosition.x-=2

            basePosition.z+=2
            basePosition.x++
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.x--
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.x--
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.x++
            basePosition.z-=2

            basePosition.x-=2
            basePosition.z--
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.z++
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.z++
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.z--
            basePosition.x+=2

            basePosition.z-=2
            basePosition.x--
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.x++
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.x++
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.x--
            basePosition.z+=2
        }
        randomPool.with { rand ->
            val height = rand.nextInt(3) + 4
            for (i in 0 until height) {
                world.setBlockOrMeta(basePosition, Log, AxialBlockState.up)
                basePosition.y++
            }
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.y++
            world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            basePosition.y--
            if (rand.nextBoolean()) { // 1 above the logs
                gen4LeavesLayer()
            } else {
                world.setBlockOrMeta(basePosition, Leaves, DefaultBlockState)
            }
            basePosition.y--
            if (rand.nextBoolean()) { // Same as top layer of logs
                gen8LeavesLayer()
            } else {
                gen4LeavesLayer()
            }
            basePosition.y--
            if (rand.nextBoolean()) { // Same as middle layer of logs
                gen17LeavesLayer()
            } else {
                gen8LeavesLayer()
            }
            basePosition.y--
            if (rand.nextBoolean()) { // Same as bottom layer of logs
                gen17LeavesLayer()
            } else {
                gen8LeavesLayer()
            }
        }
    }

    override fun generateChunk(chunk: Chunk, world: World<Chunk>) {
        var trees = 0
        randomPool.with { rand ->
            rand.setSeed(((chunk.position.x * 31L + chunk.position.y) * 31L + chunk.position.z) * 31L + seed)
            trees = rand.nextInt(treeGenPositions.size)
            for (i in 0 until trees) {
                treeGenPositions[i].apply {
                    x = rand.nextInt(Chunk.SIZE)
                    z = rand.nextInt(Chunk.SIZE)
                }
            }
        }
        blockPosPool.with { blockPos ->
            for (i in 0 until 16) {
                val x = i + chunk.position.x * Chunk.SIZE
                for (k in 0 until 16) {
                    blockPos.x = i
                    blockPos.z = k
                    val z = k + chunk.position.z * Chunk.SIZE
                    // blend \in [0, 1]
                    val blend = minOf(maxOf(generatorBlendGenerator(x.toFloat(), z.toFloat()) + 0.5f, 0f), 1f)
                    val height = blend * highFrequencyHeightGenerator(x.toFloat(), z.toFloat()) + (1 - blend) * lowFrequencyHeightGenerator(x.toFloat(), z.toFloat())
                    val dirtDepth = dirtDepthGenerator(x.toFloat(), z.toFloat()) + 2f

                    for (j in 0 until 16) {
                        val y = j + chunk.position.y * 16
                        blockPos.y = j

                        // We only generate in empty blocks (so we can write into unloaded adjacent chunks)
                        // or in leaves if they are in a bad position
                        if (chunk.getBlock(blockPos) == Air || chunk.getBlock(blockPos) == Leaves) {
                            if (y < height - dirtDepth) {
                                chunk.setBlock(blockPos, Stone, DefaultBlockState)
                            } else if (y <= floor(height).toInt()) {
                                if (floor(height).toInt() == y) {
                                    chunk.setBlock(blockPos, Grass, DefaultBlockState)
                                } else {
                                    chunk.setBlock(blockPos, Dirt, DefaultBlockState)
                                }
                            }
                        }
                    }

                    var isTree = false
                    for (t in 0 until trees) {
                        if (treeGenPositions[t].x == i && treeGenPositions[t].z == k)
                            isTree = true
                        break
                    }
                    if (isTree) {
                        val h0 = floor(height).toInt() + 1
                        if (h0 < Chunk.SIZE + chunk.position.y * Chunk.SIZE && h0 >= chunk.position.y * Chunk.SIZE) { // If it's supposed to be in this chunk
                            blockPos.x += chunk.position.x * Chunk.SIZE
                            blockPos.z += chunk.position.z * Chunk.SIZE
                            blockPos.y = h0
                            generateTree(world, blockPos)
                        }
                    }
                }
            }
        }
    }
}