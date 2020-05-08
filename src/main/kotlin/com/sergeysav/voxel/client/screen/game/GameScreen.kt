package com.sergeysav.voxel.client.screen.game

import com.sergeysav.voxel.client.Frontend
import com.sergeysav.voxel.client.FrontendProxy
import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.client.camera.CameraController
import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.client.gl.bound
import com.sergeysav.voxel.client.player.PlayerInput
import com.sergeysav.voxel.client.renderer.DefaultClientWorldRenderer
import com.sergeysav.voxel.client.renderer.OITClientWorldRenderer
import com.sergeysav.voxel.client.screen.Screen
import com.sergeysav.voxel.client.settings.GraphicsSettings
import com.sergeysav.voxel.client.world.ClientWorld
import com.sergeysav.voxel.client.world.meshing.SimpleThreadedMeshingManager
import com.sergeysav.voxel.client.world.meshing.selection.PriorityMeshSelectionStrategy
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.bound
import com.sergeysav.voxel.common.chunk.queuing.ClosestChunkQueuingStrategy
import com.sergeysav.voxel.common.world.chunks.RegionThreadedChunkManager
import com.sergeysav.voxel.common.world.generator.DevTestGenerator2
import com.sergeysav.voxel.common.world.loading.DistanceWorldLoadingStrategy
import com.sergeysav.voxel.common.world.loading.SimpleUnionWorldLoadingManager
import mu.KotlinLogging
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import kotlin.math.roundToInt

/**
 * @author sergeys
 *
 * @constructor Creates a new MainScreen
 */
class GameScreen(graphicsSettings: GraphicsSettings) : Screen {

    private val log = KotlinLogging.logger {  }
    private val cameraController = CameraController(Camera(Math.toRadians(graphicsSettings.fov).toFloat(), 1f, 1/16f, 2000f))
    private lateinit var application: Frontend
    private val blockPos = MutableBlockPosition()
    private val distanceWorldLoadingStrategy = DistanceWorldLoadingStrategy(blockPos, 10)
    private val meshingManager = SimpleThreadedMeshingManager(
        PriorityMeshSelectionStrategy(blockPos),
        parallelism = graphicsSettings.meshingSettings.parallelism,
        meshesPerFrame = graphicsSettings.meshingSettings.meshesPerFrame,
        dirtyQueueSize = graphicsSettings.meshingSettings.dirtyQueueSize
    )
    private val loadingChunkQueuingStrategy = ClosestChunkQueuingStrategy<ClientChunk>(blockPos)
    private val savingChunkQueuingStrategy = ClosestChunkQueuingStrategy<ClientChunk>(blockPos)
    private val chunkManager = RegionThreadedChunkManager(
        loadingChunkQueuingStrategy,
        savingChunkQueuingStrategy,
        DevTestGenerator2(0),
        regionFilesBasePath = graphicsSettings.chunkManagerSettings.regionFilesBasePath,
        processingQueueSize = graphicsSettings.chunkManagerSettings.loadingQueueSize,
        savingQueueSize = graphicsSettings.chunkManagerSettings.savingQueueSize,
        internalQueueSize = graphicsSettings.chunkManagerSettings.internalQueueSize,
        loadingParallelism = graphicsSettings.chunkManagerSettings.loadingParallelism,
        savingParallelism = graphicsSettings.chunkManagerSettings.savingParallelism
    )
    private val world = ClientWorld(
        SimpleUnionWorldLoadingManager(distanceWorldLoadingStrategy),
        meshingManager,
        chunkManager,
        DefaultClientWorldRenderer()
    )
    private var callback: ((Double, Double)->Boolean)? = null
    private var firstMouse = true
    private var mouseX: Double = 0.0
    private var mouseY: Double = 0.0
    private val Y = Vector3f(0f, 1f, 0f)
    private val vec1 = Vector3f()
    private val vec2 = Vector3f()
    private val playerInput = PlayerInput(
        mouseButton1JustDown = false,
        mouseButton1Down = false,
        mouseButton1JustUp = false,
        mouseButton2JustDown = false,
        mouseButton2Down = false,
        mouseButton2JustUp = false
    )
    private val debugUI = DebugUI()

    init {
        cameraController.setPos(10f, 7.5f, -5f)
        cameraController.lookAt(-5f, -5f, 15f)
    }

    override fun register(application: Frontend) {
        this.application = application

        log.trace { "Setting up OpenGL Context for Main rendering" }
        GL11.glClearColor(0f, 0f, 0.2f, 0f)

        log.trace { "Setting up Frontend inputs" }
        application.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)

        callback = application.mouseMoveCallbacks.add(1) { x, y ->
            mouseX = x
            mouseY = y
            false
        }

        application.mouseButtonCallbacks.add(1) { button: Int, action: Int, mods: Int, x: Double, y: Double ->
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW.GLFW_PRESS) {
                    playerInput.mouseButton1JustDown = true
                    playerInput.mouseButton1Down = true
                } else if (action == GLFW.GLFW_RELEASE) {
                    playerInput.mouseButton1Down = false
                    playerInput.mouseButton1JustUp = true
                }
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (action == GLFW.GLFW_PRESS) {
                    playerInput.mouseButton2JustDown = true
                    playerInput.mouseButton2Down = true
                } else if (action == GLFW.GLFW_RELEASE) {
                    playerInput.mouseButton2Down = false
                    playerInput.mouseButton2JustUp = true
                }
            }
            false
        }
    }

    override fun render(delta: Double) {
        debugUI.layout(application.gui, 1/delta, meshingManager.getQueueSize(), loadingChunkQueuingStrategy.currentSize(), savingChunkQueuingStrategy.currentSize())

        val speed = 0.2f
        val forwardBackward = speed * (if (application.isKeyPressed(GLFW.GLFW_KEY_W)) 1 else 0 + if (application.isKeyPressed(
                GLFW.GLFW_KEY_S)) -1 else 0) * delta * 60
        val upDown = speed * (if (application.isKeyPressed(GLFW.GLFW_KEY_SPACE)) 1 else 0 + if (application.isKeyPressed(
                GLFW.GLFW_KEY_LEFT_SHIFT)) -1 else 0) * delta * 60
        val rightLeft = speed * (if (application.isKeyPressed(GLFW.GLFW_KEY_D)) 1 else 0 + if (application.isKeyPressed(
                GLFW.GLFW_KEY_A)) -1 else 0) * delta * 60

        cameraController.run {
            setAspect(application.fWidth, application.fHeight)
            val dot = vec1.set(forward).dot(Y)
            vec2.set(Y).mul(dot)
            vec1.sub(vec2)
            translate(vec1.normalize(), forwardBackward.toFloat())
            translate(right, rightLeft.toFloat())
            translate(Y, upDown.toFloat())
            if (!firstMouse) {
                rotate(right, (mouseY - application.height / 2.0).toFloat() * 0.01f * 0.5f)
                rotate(Y, (mouseX - application.width / 2.0).toFloat() * 0.01f * 0.5f)
            }
            application.setCursorPosition(application.width / 2.0, application.height / 2.0)
            mouseX = application.width / 2.0
            mouseY = application.height / 2.0
            firstMouse = false

            update()
        }

        blockPos.y = cameraController.camera.position.y().roundToInt()//.divisionQuotient(Chunk.SIZE)
        blockPos.x = cameraController.camera.position.x().roundToInt()//.divisionQuotient(Chunk.SIZE)
        blockPos.z = cameraController.camera.position.z().roundToInt()//.divisionQuotient(Chunk.SIZE)

        world.update()
        world.draw(cameraController.camera, playerInput, application.fWidth, application.fHeight)

        FrontendProxy.crosshairShader.bound {
            FrontendProxy.crosshairTexture.bound(0) {
                GL20.glUniform1i(FrontendProxy.crosshairShader.getUniform("crosshairImage"), 0)
                GL20.glUniform2f(FrontendProxy.crosshairShader.getUniform("frameSize"), application.fWidth.toFloat(), application.fHeight.toFloat())
                GL20.glUniform1f(FrontendProxy.crosshairShader.getUniform("crosshairSize"), 16f)
                FrontendProxy.screenMesh.draw()
            }
        }

        playerInput.mouseButton1JustDown = false
        playerInput.mouseButton1JustUp = false
        playerInput.mouseButton2JustDown = false
        playerInput.mouseButton2JustUp = false

//        GL11.glClearColor(0f, 0f, 0.2f, 0f) // Reset clear color
    }

    override fun unregister(application: Frontend) {
        callback?.let { application.mouseMoveCallbacks.remove(it) }
    }

    override fun cleanup() {
        log.trace { "Cleaning up Main rendering screen" }
        world.cleanup()
    }
}