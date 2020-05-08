package com.sergeysav.voxel.client

import com.sergeysav.voxel.client.block.AxialSolidBlockMesher
import com.sergeysav.voxel.client.block.ClientBlockMesher
import com.sergeysav.voxel.client.block.GrassBlockMesher
import com.sergeysav.voxel.client.block.LeavesBlockMesher
import com.sergeysav.voxel.client.block.NoMeshBlockMesher
import com.sergeysav.voxel.client.block.RandomizedSolidBlockMesher
import com.sergeysav.voxel.client.block.SolidBlockMesher
import com.sergeysav.voxel.client.block.TestBlockMesher
import com.sergeysav.voxel.client.block.WaterBlockMesher
import com.sergeysav.voxel.client.gl.GLDataUsage
import com.sergeysav.voxel.client.gl.GLDrawingMode
import com.sergeysav.voxel.client.gl.Image
import com.sergeysav.voxel.client.gl.Mesh
import com.sergeysav.voxel.client.gl.ShaderProgram
import com.sergeysav.voxel.client.gl.Texture2D
import com.sergeysav.voxel.client.gl.TextureInterpolationMode
import com.sergeysav.voxel.client.gl.Vec2VertexAttribute
import com.sergeysav.voxel.client.gl.createTexture
import com.sergeysav.voxel.client.resource.texture.AtlasDataTexture
import com.sergeysav.voxel.client.resource.texture.TextureAtlas
import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.CommonProxy
import com.sergeysav.voxel.common.IOUtil
import com.sergeysav.voxel.common.MainThreadRunner
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.block.impl.Dirt
import com.sergeysav.voxel.common.block.impl.Grass
import com.sergeysav.voxel.common.block.impl.Leaves
import com.sergeysav.voxel.common.block.impl.Log
import com.sergeysav.voxel.common.block.impl.Stone
import com.sergeysav.voxel.common.block.impl.Test
import com.sergeysav.voxel.common.block.impl.Water
import com.sergeysav.voxel.common.block.state.BlockState
import mu.KotlinLogging

/**
 * @author sergeys
 */
object FrontendProxy : CommonProxy() {

    private val log = KotlinLogging.logger {  }
    private val textures = mutableListOf<TextureResource>()
    private val missingMesher = SolidBlockMesher<Block<BlockState>, BlockState>(getTextureResource("missing"))
    private val blockMeshers = mutableMapOf<Block<*>, ClientBlockMesher<*, *>>()

    lateinit var voxelShader: ShaderProgram
        private set
    lateinit var translucentVoxelShader: ShaderProgram
        private set
    lateinit var crosshairShader: ShaderProgram
        private set
    lateinit var compositionShader: ShaderProgram
        private set
    lateinit var passthroughShader: ShaderProgram
        private set
    var textureAtlas: Texture2D = Texture2D(0)
        private set
    var assetData: Texture2D = Texture2D(0)
        private set
    var crosshairTexture: Texture2D = Texture2D(0)
        private set
    lateinit var screenMesh: Mesh
        private set

    fun <B : Block<out S>, S : BlockState> registerBlockMesher(block: B, mesher: ClientBlockMesher<B, S>) {
        log.trace { "Loading Mesher for ${block.unlocalizedName}" }
        blockMeshers[block] = mesher
    }

    fun getTextureResource(resource: String): TextureResource {
        return textures.firstOrNull { it.resourceName == resource } ?: TextureResource(textures.size, resource).also {
            textures.add(it)
        }
    }

    override fun initialize(mainThreadRunner: MainThreadRunner) {
        super.initialize(mainThreadRunner)

        registerBlockMesher(Air, NoMeshBlockMesher)
        registerBlockMesher(Dirt, RandomizedSolidBlockMesher(getTextureResource("dirt")))
        registerBlockMesher(Stone, RandomizedSolidBlockMesher(getTextureResource("stone")))
        registerBlockMesher(Grass, GrassBlockMesher(
            up = getTextureResource("grass_top"),
            down = getTextureResource("dirt"),
            side = getTextureResource("grass_side")
        ))
        registerBlockMesher(Test, TestBlockMesher(getTextureResource("test")))
        registerBlockMesher(Log, AxialSolidBlockMesher(
            getTextureResource("log_side"),
            getTextureResource("log_section")
        ))
        registerBlockMesher(Leaves, LeavesBlockMesher(getTextureResource("leaves")))
        registerBlockMesher(Water, WaterBlockMesher(getTextureResource("water")))

        log.trace { "Loading Opaque Voxel Shader" }
        val voxelVertexShader = IOUtil.loadResource("/shaders/voxel.vertex.glsl")
        val voxelFragmentShader = IOUtil.loadResource("/shaders/voxel.fragment.glsl")
        val shaderLoader = mainThreadRunner.runOnMainThread {
            val voxelShader = ShaderProgram()
            voxelShader.createVertexShader(voxelVertexShader)
            voxelShader.createFragmentShader(voxelFragmentShader)
            voxelShader.link()
            voxelShader
        }

        log.trace { "Loading Translucent Voxel Shader" }
        val transparentVoxelShader = IOUtil.loadResource("/shaders/default_transparent.fragment.glsl")
        val translucentVoxelShader = mainThreadRunner.runOnMainThread {
            val shader = ShaderProgram()
            shader.createVertexShader(voxelVertexShader)
            shader.createFragmentShader(transparentVoxelShader)
            shader.link()
            shader
        }

        log.trace { "Loading Atlas Texture and Data" }
        val assetDataTexture = AtlasDataTexture(128, 128)
        val atlasLoader = TextureAtlas.loadToAtlas(
            textures.map { "/images/${it.resourceName}.png" },
            512, 512, 0, assetDataTexture.textureData,
            mainThreadRunner
        )
        val assetDataLoader = mainThreadRunner.runOnMainThread {
            assetDataTexture.createTexture()
        }

        log.trace { "Loading Crosshair Image" }
        val crosshairsImage = Image.load(IOUtil.readResourceToBuffer("/images/crosshairs_inverted.png", 1000))
        val crosshairTextureLoader = mainThreadRunner.runOnMainThread {
            log.trace { "Creating Crosshair Texture" }
            val crosshairTexture = crosshairsImage.createTexture(
                minInterp = TextureInterpolationMode.NEAREST,
                maxInterp = TextureInterpolationMode.NEAREST
            )
            crosshairsImage.free()
            crosshairTexture
        }

        log.trace { "Loading Crosshair Shader GLSL" }
        val crosshairVertexGLSL = IOUtil.loadResource("/shaders/crosshair.vertex.glsl")
        val crosshairFragmentGLSL = IOUtil.loadResource("/shaders/crosshair.fragment.glsl")
        val crosshairShaderLoader = mainThreadRunner.runOnMainThread {
            log.trace { "Creating Crosshair Shader" }
            val crosshairShader = ShaderProgram()
            crosshairShader.createVertexShader(crosshairVertexGLSL)
            crosshairShader.createFragmentShader(crosshairFragmentGLSL)
            crosshairShader.link()
            crosshairShader
        }

        log.trace { "Loading Screen Shader GLSL" }
        val screenVertexGLSL = IOUtil.loadResource("/shaders/screen.vertex.glsl")
        val screenFragmentGLSL = IOUtil.loadResource("/shaders/screen.fragment.glsl")
        val screenShaderLoader = mainThreadRunner.runOnMainThread {
            log.trace { "Creating Screen Shader" }
            val screenShader = ShaderProgram()
            screenShader.createVertexShader(screenVertexGLSL)
            screenShader.createFragmentShader(screenFragmentGLSL)
            screenShader.link()
            screenShader
        }

        log.trace { "Loading Passthrough GLSL" }
        val passthroughFragmentGLSL = IOUtil.loadResource("/shaders/passthrough.fragment.glsl")
        val passthroughShaderLoader = mainThreadRunner.runOnMainThread {
            log.trace { "Creating Passthrough Shader" }
            val screenShader = ShaderProgram()
            screenShader.createVertexShader(screenVertexGLSL)
            screenShader.createFragmentShader(passthroughFragmentGLSL)
            screenShader.link()
            screenShader
        }

        val screenMeshLoader = mainThreadRunner.runOnMainThread {
            log.trace { "Creating Screen Mesh" }
            val crosshairMesh = Mesh(GLDrawingMode.TRIANGLES, true)
            crosshairMesh.setVertices(floatArrayOf(
                -1f, 1f, 0f, 1f,
                1f, 1f, 1f, 1f,
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f
            ), GLDataUsage.STATIC, Vec2VertexAttribute("aPos"), Vec2VertexAttribute("aUV")
            )
            crosshairMesh.setIndexData(intArrayOf(
                0, 2, 1,
                2, 3, 1
            ), GLDataUsage.STATIC)
            crosshairMesh
        }

        crosshairShader = crosshairShaderLoader.get()
        compositionShader = screenShaderLoader.get()
        passthroughShader = passthroughShaderLoader.get()
        screenMesh = screenMeshLoader.get()
        val crosshairMeshVerifier = mainThreadRunner.runOnMainThread {
            log.trace { "Verifying Screen Mesh Shaders" }
            screenMesh.bound {
                crosshairShader.validate()
                compositionShader.validate()
                passthroughShader.validate()
            }
        }

        voxelShader = shaderLoader.get()
        this.translucentVoxelShader = translucentVoxelShader.get()
        textureAtlas = atlasLoader.get()
        crosshairTexture = crosshairTextureLoader.get()
        assetData = assetDataLoader.get()
        crosshairMeshVerifier.get()
    }

    fun cleanup() {
        if (this::voxelShader.isInitialized) {
            voxelShader.cleanup()
        }
        if (this::translucentVoxelShader.isInitialized) {
            translucentVoxelShader.cleanup()
        }
        if (this::compositionShader.isInitialized) {
            compositionShader.cleanup()
        }
        if (this::passthroughShader.isInitialized) {
            passthroughShader.cleanup()
        }
        textureAtlas.cleanup()
        assetData.cleanup()

        if (this::screenMesh.isInitialized) {
            screenMesh.cleanup()
        }
        if (this::crosshairShader.isInitialized) {
            crosshairShader.cleanup()
        }
        crosshairTexture.cleanup()
    }

    override fun <B : Block<out S>, S : BlockState> getProxyBlockMesher(block: B): ClientBlockMesher<B, S> {
        @Suppress("UNCHECKED_CAST")
        return (blockMeshers[block] ?: missingMesher) as ClientBlockMesher<B, S>
    }
}