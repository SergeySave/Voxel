package com.sergeysav.voxel.client

import com.sergeysav.voxel.client.block.ClientBlockMesher
import com.sergeysav.voxel.client.block.NoMeshBlockMesher
import com.sergeysav.voxel.client.block.RandomizedSolidBlockMesher
import com.sergeysav.voxel.client.block.GrassBlockMesher
import com.sergeysav.voxel.client.block.SolidBlockMesher
import com.sergeysav.voxel.client.gl.ShaderProgram
import com.sergeysav.voxel.client.gl.Texture2D
import com.sergeysav.voxel.client.resource.texture.AtlasDataTexture
import com.sergeysav.voxel.client.resource.texture.TextureAtlas
import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.CommonProxy
import com.sergeysav.voxel.common.IOUtil
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.block.impl.Dirt
import com.sergeysav.voxel.common.block.impl.Grass
import com.sergeysav.voxel.common.block.impl.Stone
import com.sergeysav.voxel.common.block.impl.Test
import com.sergeysav.voxel.common.block.state.BlockState

/**
 * @author sergeys
 */
object FrontendProxy : CommonProxy() {

    private val textures = mutableListOf<TextureResource>()
    private val missingMesher = SolidBlockMesher<Block<BlockState>, BlockState>(getTextureResource("missing"))
    private val blockMeshers = mutableMapOf<Block<*>, ClientBlockMesher<*, *>>()

    lateinit var voxelShader: ShaderProgram
        private set
    var textureAtlas: Texture2D = Texture2D(0)
        private set
    var assetData: Texture2D = Texture2D(0)
        private set

    init {
        registerBlockMesher(Air, NoMeshBlockMesher)
        registerBlockMesher(Dirt, RandomizedSolidBlockMesher(getTextureResource("dirt")))
        registerBlockMesher(Stone, RandomizedSolidBlockMesher(getTextureResource("stone")))
        registerBlockMesher(Grass, GrassBlockMesher(
            up = getTextureResource("grass_top"),
            down = getTextureResource("dirt"),
            side = getTextureResource("grass_side")
        ))
        registerBlockMesher(Test, SolidBlockMesher(getTextureResource("test")))
    }

    fun <B : Block<out S>, S : BlockState> registerBlockMesher(block: B, mesher: ClientBlockMesher<B, S>) {
        blockMeshers[block] = mesher
    }

    fun getTextureResource(resource: String): TextureResource {
        return textures.firstOrNull { it.resourceName == resource } ?: TextureResource(textures.size, resource).also {
            textures.add(it)
        }
    }

    fun initialize() {
        voxelShader = ShaderProgram()
        voxelShader.createVertexShader(IOUtil.loadResource("/shaders/voxel.vertex.glsl"))
        voxelShader.createFragmentShader(IOUtil.loadResource("/shaders/voxel.fragment.glsl"))
        voxelShader.link()

        val assetDataTexture = AtlasDataTexture(128, 128)
        textureAtlas = TextureAtlas.loadToAtlas(
            textures.map { "/images/${it.resourceName}.png" },
            512, 512, 0, assetDataTexture.textureData)

        assetData = assetDataTexture.createTexture()
    }

    fun cleanup() {
        voxelShader.cleanup()
        textureAtlas.cleanup()
        assetData.cleanup()
    }

    override fun <B : Block<out S>, S : BlockState> getProxyBlockMesher(block: B): ClientBlockMesher<B, S> {
        @Suppress("UNCHECKED_CAST")
        return (blockMeshers[block] ?: missingMesher) as ClientBlockMesher<B, S>
    }
}