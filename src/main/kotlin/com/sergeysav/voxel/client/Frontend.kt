package com.sergeysav.voxel.client

import com.sergeysav.voxel.client.glfw.GLFWManager
import com.sergeysav.voxel.client.screen.main.MainScreen
import com.sergeysav.voxel.client.screen.Screen
import mu.KotlinLogging
import org.lwjgl.opengl.GL11
import java.util.Deque
import java.util.LinkedList

/**
 * @author sergeys
 *
 * @constructor Creates a new Frontend
 */
class Frontend : GLFWManager(800, 600) {
    private val log = KotlinLogging.logger {}

    private var lastNano = 0L
    private var screenStack: Deque<Screen> = LinkedList()
    private val sync = Sync()

    init {
        log.info { "Voxel frontend starting up" }
    }

    override fun create() {

    }

    override fun init() {
        GL11.glClearColor(0f, 0f, 0f, 0f)

        log.info { "Initializing Frontend data" }
        FrontendProxy.initialize()

        log.trace { "Showing Main Screen" }
        openScreen(MainScreen())
    }

    override fun render() {
        val now = System.nanoTime()
        val delta = ((now - lastNano) / 1.0e9)
        lastNano = now

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

        screenStack.peek()?.render(delta)

        sync(60)
    }

    override fun cleanup() {
        log.info { "Cleaning up Frontend" }
        screenStack.peek().unregister(this)
        while (screenStack.isNotEmpty()) {
            screenStack.pop().cleanup()
        }
        FrontendProxy.cleanup()
    }

    fun openScreen(screen: Screen) {
        screenStack.peek()?.unregister(this)
        screenStack.push(screen)
        screen.register(this)
    }

    /**
     * Note this will NOT cause the screen to be cleaned up
     * In order to clean the screen up use destroyScreen
     *
     * If this is called from the current screen then the returned result should be this
     */
    fun popScreen(): Screen? {
        val screen: Screen? = screenStack.pop()
        screen?.unregister(this)
        screenStack.peek()?.register(this)
        return screen
    }

    /**
     * Note this WILL cause the screen to be cleaned up
     * In order to not clean the screen up use popScreen
     */
    fun destroyScreen() {
        val screen: Screen? = screenStack.pop()
        screen?.unregister(this)
        screenStack.peek()?.register(this)
        screen?.cleanup()
    }
}