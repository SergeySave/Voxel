package com.sergeysav.voxel.client.glfw

import com.sergeysav.voxel.client.nuklear.Gui
import mu.KotlinLogging
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.nuklear.Nuklear
import org.lwjgl.opengl.ARBDebugOutput
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL43
import org.lwjgl.opengl.GLUtil
import org.lwjgl.opengl.KHRDebug
import org.lwjgl.system.Callback
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import java.nio.IntBuffer

/**
 * @author sergeys
 *
 * @constructor Creates a new GLFWManager
 */
abstract class GLFWManager(width: Int, height: Int): InputManager() {
    private val log = KotlinLogging.logger {}

    private var window: Long = MemoryUtil.NULL

    var width: Int = width
        private set

    var height: Int = height
        private set

    var fWidth: Int = width
        private set

    var fHeight: Int = height
        private set

    private var debugProc: Callback? = null
    lateinit var gui: Gui

    fun run(title: String) {
        log.info { "LWJGL ${Version.getVersion()} GLFWManager Starting" }

        try {
            init(width, height, title)
            loop()
        } catch (e: Exception) {
            log.error(e) { "Error Occurred" }
        } finally {
            log.debug { "Closing" }
            cleanup()
            gui.cleanup()
            Callbacks.glfwFreeCallbacks(window)
            debugProc?.free()
            GLFW.glfwDestroyWindow(window)
            GLFW.glfwTerminate()
            GLFW.glfwSetErrorCallback(null)?.free()
        }
    }

    private fun init(width: Int, height: Int, title: String) {
        log.debug { "Initializing" }
        GLFWErrorCallback.createPrint(System.err).set()

        if ( !GLFW.glfwInit())
            throw IllegalStateException("Unable to initialize GLFW")

        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default

        //Request minimum OpenGL 3.3
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3)

        //Allow the usage of forward compatability
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)

        //Get a core OpenGL profile
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)

        //Don't show the winow as soon as it is created
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)

        //Allow the window to be resized
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)

        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE)

        log.trace { "Creating application" }
        create()

        log.trace { "Creating window" }
        window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL)
            throw IllegalStateException("Failed to create the GLFW window")

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window)

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        val caps = GL.createCapabilities(true)

        debugProc = GLUtil.setupDebugMessageCallback()
        when {
            caps.OpenGL43            -> GL43.glDebugMessageControl(GL43.GL_DEBUG_SOURCE_API, GL43.GL_DEBUG_TYPE_OTHER,
                GL43.GL_DEBUG_SEVERITY_NOTIFICATION,
                null as IntBuffer?, false)
            caps.GL_KHR_debug        -> KHRDebug.glDebugMessageControl(
                KHRDebug.GL_DEBUG_SOURCE_API,
                KHRDebug.GL_DEBUG_TYPE_OTHER,
                KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION,
                null as IntBuffer?,
                false
            )
            caps.GL_ARB_debug_output -> ARBDebugOutput.glDebugMessageControlARB(ARBDebugOutput.GL_DEBUG_SOURCE_API_ARB,
                ARBDebugOutput.GL_DEBUG_TYPE_OTHER_ARB,
                ARBDebugOutput.GL_DEBUG_SEVERITY_LOW_ARB,
                null as IntBuffer?, false)
        }

        log.trace { "Adding callbacks" }
        GLFW.glfwSetFramebufferSizeCallback(window) { _, w, h -> this.resize(w, h) }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        GLFW.glfwSetKeyCallback(window) { _, key, scanCode, action, mods ->
            handleKeyCallback(key, scanCode, action, mods)
        }

        GLFW.glfwSetMouseButtonCallback(window) { window, button, action, mods ->
            var x: Double = -1.0
            var y: Double = -1.0
            MemoryStack.stackPush().use { stack ->
                val pX = stack.mallocDouble(1)
                val pY = stack.mallocDouble(1)

                GLFW.glfwGetCursorPos(window, pX, pY)
                x = pX.get()
                y = pY.get()
            }
            handleMouseButtonCallback(button, action, mods, x, y)
        }

        GLFW.glfwSetCursorPosCallback(window) { _, x, y -> handleMouseMoveCallback(x, y) }

        GLFW.glfwSetScrollCallback(window) { _, x, y -> handleScrollCallback(x, y) }

        GLFW.glfwSetCharCallback(window) { _, codePoint -> handleCharacterCallback(codePoint) }

        gui = Gui(this, "/font/Inconsolata/Inconsolata-VariableFont_wdth,wght.ttf")

        log.trace { "Setting window contexts" }
        // Get the thread stack and push a new frame
        MemoryStack.stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            GLFW.glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())

            // Center the window
            GLFW.glfwSetWindowPos(
                window,
                (vidmode!!.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            )
        } // the stack frame is popped automatically

        // Enable v-sync
        GLFW.glfwSwapInterval(1)

        // Make the window visible
        GLFW.glfwShowWindow(window)
        GLFW.glfwFocusWindow(window)

        log.debug { "Initializing application" }
        init()
    }

    private fun loop() {
        log.debug { "Starting Render Loop" }
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!GLFW.glfwWindowShouldClose(window)) {
            MemoryStack.stackPush().use { stack ->
                val w = stack.mallocInt(1)
                val h = stack.mallocInt(1)

                GLFW.glfwGetWindowSize(window, w, h)
                width = w.get(0)
                height = h.get(0)

                GLFW.glfwGetFramebufferSize(window, w, h)
                fWidth = w.get(0)
                fHeight = h.get(0)
                GL11C.glViewport(0, 0, fWidth, fHeight)
            }

            gui.doRegisterInputEvents()

            render()
            gui.render(Nuklear.NK_ANTI_ALIASING_ON, 512 * 1024, 128 * 1024, width, height, fWidth, fHeight)

            GLFW.glfwSwapBuffers(window) // swap the color bufferobjects
        }
        log.debug { "Ending Render Loop" }
    }

    override fun isKeyPressed(key: Int) = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS
    override fun isMouseDown(button: Int): Boolean = GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS
    override fun setClipboardString(string: ByteBuffer) {
        GLFW.glfwSetClipboardString(window, string)
    }

    override fun getClipboardString(): Long = GLFW.nglfwGetClipboardString(window)
    override fun checkForInputEvents() = GLFW.glfwPollEvents()
    override fun setInputMode(mode: Int, value: Int) = GLFW.glfwSetInputMode(window, mode, value)
    override fun setCursorPosition(x: Double, y: Double) = GLFW.glfwSetCursorPos(window, x, y)
    override fun getCursorPosition(x: DoubleBuffer, y: DoubleBuffer) {
        GLFW.glfwGetCursorPos(window, x, y)
    }

    protected abstract fun create()
    protected abstract fun init()
    protected abstract fun render()
    protected abstract fun cleanup()

    protected open fun resize(width: Int, height: Int) = Unit
}