package com.sergeysav.voxel.client.chunk.meshing

import com.sergeysav.voxel.client.block.ClientBlockMesher
import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.client.gl.GLDataUsage
import com.sergeysav.voxel.client.gl.UVec3VertexAttribute
import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.Voxel
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.data.Direction
import com.sergeysav.voxel.common.world.World
import org.lwjgl.BufferUtils
import java.nio.IntBuffer

/**
 * @author sergeys
 *
 * @constructor Creates a new AdjacentChunkMesher
 */
class SplittingChunkMesher(
    private val shortCircuitCallback: (SplittingChunkMesher)->Unit
) : ChunkMesher {

    private lateinit var chunk: ClientChunk
    private lateinit var world: World<*>
    private val blockPos = MutableBlockPosition()
    private val globalBlockPos = MutableBlockPosition()
    private val blockPos2 = MutableBlockPosition()
    private val clientChunkPackedVertexDataAttribute = UVec3VertexAttribute("packedVertexData")
    private val opaqueCallback = MeshingCallback()
    private val translucentCallback = MeshingCallback()
//    private var transparent: Boolean = false

    override var free: Boolean = true
    override var ready: Boolean = false
        private set

    init {
        opaqueCallback.transparent = false
        translucentCallback.transparent = true
    }

    override fun generateMesh(world: World<*>, chunk: ClientChunk) {
        free = false
        opaqueCallback.reset()
        translucentCallback.reset()
        this.chunk = chunk
        this.world = world

        for (x in 0 until 16) {
            for (y in 0 until 16) {
                for (z in 0 until 16) {
                    blockPos.x = x
                    blockPos.y = y
                    blockPos.z = z
                    @Suppress("UNCHECKED_CAST")
                    val block: Block<BlockState> = chunk.getBlock(blockPos) as Block<BlockState>
                    val blockState: BlockState = chunk.getBlockState(blockPos)
                    globalBlockPos.set(blockPos)
                    globalBlockPos.x += chunk.position.x * Chunk.SIZE
                    globalBlockPos.y += chunk.position.y * Chunk.SIZE
                    globalBlockPos.z += chunk.position.z * Chunk.SIZE
                    val mesher = Voxel.getBlockMesher<ChunkMesherCallback, Block<BlockState>, BlockState>(block) as ClientBlockMesher<Block<BlockState>, BlockState>?
                    mesher?.addToMesh(opaqueCallback, translucentCallback, globalBlockPos, block, blockState)
                }
            }
        }

        if (opaqueCallback.indices == 0 && translucentCallback.indices == 0 && chunk.isMeshEmpty) {
            // Empty chunks, whose meshes were empty last time
            ready = false
            free = true
            shortCircuitCallback(this)
        } else {
            opaqueCallback.limit()
            translucentCallback.limit()
            ready = true
        }
    }

    override fun applyMesh() {
        ready = false

        var opaqueMesh = chunk.opaqueMesh
        if (opaqueMesh == null) {
            opaqueMesh = ClientChunk.meshPool.get()
            chunk.opaqueMesh = opaqueMesh
        }
        if (!opaqueMesh.fullyInitialized) {
            opaqueMesh.setVertices(opaqueCallback.vertexData, GLDataUsage.STATIC, clientChunkPackedVertexDataAttribute)
        } else {
            opaqueMesh.setVertexData(opaqueCallback.vertexData)
        }
        opaqueMesh.setIndexData(opaqueCallback.indexData, GLDataUsage.STATIC, opaqueCallback.indices)

        var translucentMesh = chunk.translucentMesh
        if (translucentMesh == null) {
            translucentMesh = ClientChunk.meshPool.get()
            chunk.translucentMesh = translucentMesh
        }
        if (!translucentMesh.fullyInitialized) {
            translucentMesh.setVertices(translucentCallback.vertexData, GLDataUsage.STATIC, clientChunkPackedVertexDataAttribute)
        } else {
            translucentMesh.setVertexData(translucentCallback.vertexData)
        }
        translucentMesh.setIndexData(translucentCallback.indexData, GLDataUsage.STATIC, translucentCallback.indices)

        chunk.isMeshEmpty = opaqueCallback.indices == 0 && translucentCallback.indices == 0

        free = true
    }

    private fun putVertex(data: IntBuffer,
                  subX: Int, subY: Int, subZ: Int,
                  imageIndex: Int,
                  facingAxis: Int,
                  rotation: Int,
                  reflection: Int,
                  lightingR: Int, lightingG: Int, lightingB: Int) {
        data.put( ((subX and 0xFFFF) shl 16) or (subY and 0xFFFF) )
        data.put( ((subZ and 0xFFFF) shl 16) or ((imageIndex and 0xFFFF0) ushr 4) )
        data.put( ((imageIndex and 0x0000F) shl 28) or
                ((facingAxis and 0b111) shl 25) or
                ((rotation and 0b11) shl 23) or
                ((reflection and 0b11) shl 21) or ((lightingR and 0x7F) shl 14) or ((lightingG and 0x7F) shl 7) or (lightingB and 0x7F))
    }

    inner class MeshingCallback : ChunkMesherCallback {
        var vertexData: IntBuffer = BufferUtils.createIntBuffer(16*16*16*72)
        var indexData: IntBuffer = BufferUtils.createIntBuffer(16*16*16*36)
        var vertices = 0
        var indices = 0
        var transparent = false

        fun reset() {
            vertexData.rewind()
            indexData.rewind()
            vertexData.limit(vertexData.capacity())
            indexData.limit(indexData.capacity())
            vertices = 0
            indices = 0
        }

        fun limit() {
            vertexData.rewind()
            indexData.rewind()
            vertexData.limit(vertices * 3) // 3 data points per vertex
            indexData.limit(indices)
        }

        override fun addVertex(
            x: Double, y: Double, z: Double,
            texture: TextureResource,
            facing: Direction, rotation: BlockTextureRotation, reflection: BlockTextureReflection,
            lightingR: Double, lightingG: Double, lightingB: Double
        ): Int {
            putVertex(
                vertexData,
                (blockPos.x shl 12) + (x * 0xFFF + 0.5).toInt(),
                (blockPos.y shl 12) + (y * 0xFFF + 0.5).toInt(),
                (blockPos.z shl 12) + (z * 0xFFF + 0.5).toInt(),
                texture.imageIndex,
                facing.textureAxis,
                rotation.ordinal,
                reflection.ordinal,
                (0x7F * lightingR + 0.5).toInt(),
                (0x7F * lightingG + 0.5).toInt(),
                (0x7F * lightingB + 0.5).toInt()
            )
            return vertices++
        }

        override fun addTriangle(vertex1: Int, vertex2: Int, vertex3: Int) {
            indexData.put(vertex1)
            indexData.put(vertex2)
            indexData.put(vertex3)
            indices += 3
        }

        override fun addAAQuad(inset: Double,
                               l1: Double, u1: Double, r1: Double, g1: Double, b1: Double,
                               l2: Double, u2: Double, r2: Double, g2: Double, b2: Double,
                               l3: Double, u3: Double, r3: Double, g3: Double, b3: Double,
                               l4: Double, u4: Double, r4: Double, g4: Double, b4: Double,
                               texture: TextureResource, facing: Direction, rotation: BlockTextureRotation,
                               reflection: BlockTextureReflection, border: Boolean, doubleRender: Boolean) {
            // Adjacency optimization
            if (border) {
                blockPos2.x = blockPos.x + facing.relX + chunk.position.x * 16
                blockPos2.y = blockPos.y + facing.relY + chunk.position.y * 16
                blockPos2.z = blockPos.z + facing.relZ + chunk.position.z * 16
                val block = world.getBlock(blockPos2)
                if (block != null) {
                    @Suppress("UNCHECKED_CAST")
                    val mesher = Voxel.getBlockMesher<ChunkMesherCallback, Block<BlockState>, BlockState>(block as Block<BlockState>) as ClientBlockMesher<Block<BlockState>, BlockState>?
                    if (mesher?.shouldOpaqueAdjacentHideFace(facing.opposite) == true) return
                    if (transparent && mesher?.shouldTransparentAdjacentHideFace(facing.opposite) == true) return
                }
            }
            // Transparent things should be rendered into both meshes
            if (transparent && doubleRender) opaqueCallback.addAAQuad(inset, l1, u1, r1, g1, b1, l2, u2, r2, g2, b2, l3, u3, r3, g3, b3, l4, u4, r4, g4, b4, texture, facing, rotation, reflection, border, false)

            val baseX = facing.relX * 0.5 + facing.left.opposite.relX * 0.5 + facing.up.opposite.relX * 0.5 + 0.5
            val baseY = facing.relY * 0.5 + facing.left.opposite.relY * 0.5 + facing.up.opposite.relY * 0.5 + 0.5
            val baseZ = facing.relZ * 0.5 + facing.left.opposite.relZ * 0.5 + facing.up.opposite.relZ * 0.5 + 0.5

            val v1 = addVertex(
                baseX + facing.opposite.relX * inset + facing.left.relX * l1 + facing.up.relX * u1,
                baseY + facing.opposite.relY * inset + facing.left.relY * l1 + facing.up.relY * u1,
                baseZ + facing.opposite.relZ * inset + facing.left.relZ * l1 + facing.up.relZ * u1,
                texture, facing, rotation, reflection, r1, g1, b1
            )
            val v2 = addVertex(
                baseX + facing.opposite.relX * inset + facing.left.relX * l2 + facing.up.relX * u2,
                baseY + facing.opposite.relY * inset + facing.left.relY * l2 + facing.up.relY * u2,
                baseZ + facing.opposite.relZ * inset + facing.left.relZ * l2 + facing.up.relZ * u2,
                texture, facing, rotation, reflection, r2, g2, b2
            )
            val v3 = addVertex(
                baseX + facing.opposite.relX * inset + facing.left.relX * l3 + facing.up.relX * u3,
                baseY + facing.opposite.relY * inset + facing.left.relY * l3 + facing.up.relY * u3,
                baseZ + facing.opposite.relZ * inset + facing.left.relZ * l3 + facing.up.relZ * u3,
                texture, facing, rotation, reflection, r3, g3, b3
            )
            val v4 = addVertex(
                baseX + facing.opposite.relX * inset + facing.left.relX * l4 + facing.up.relX * u4,
                baseY + facing.opposite.relY * inset + facing.left.relY * l4 + facing.up.relY * u4,
                baseZ + facing.opposite.relZ * inset + facing.left.relZ * l4 + facing.up.relZ * u4,
                texture, facing, rotation, reflection, r4, g4, b4
            )

            addTriangle(v1, v2, v3)
            addTriangle(v3, v2, v4)
        }
    }
}