package com.sergeysav.voxel.client.nuklear

import com.sergeysav.voxel.client.gl.ElementBufferObject
import com.sergeysav.voxel.client.gl.ShaderProgram
import com.sergeysav.voxel.client.gl.Texture2D
import com.sergeysav.voxel.client.gl.VertexArrayObject
import com.sergeysav.voxel.client.gl.VertexBufferObject
import com.sergeysav.voxel.client.gl.bound
import com.sergeysav.voxel.client.glfw.InputManager
import com.sergeysav.voxel.common.IOUtil
import com.sergeysav.voxel.common.bound
import mu.KotlinLogging
import org.lwjgl.glfw.GLFW
import org.lwjgl.nuklear.NkAllocator
import org.lwjgl.nuklear.NkBuffer
import org.lwjgl.nuklear.NkContext
import org.lwjgl.nuklear.NkConvertConfig
import org.lwjgl.nuklear.NkDrawNullTexture
import org.lwjgl.nuklear.NkDrawVertexLayoutElement
import org.lwjgl.nuklear.NkVec2
import org.lwjgl.nuklear.Nuklear
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL14C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Platform
import java.nio.ByteBuffer
import java.util.Objects

/**
 * @author sergeys
 *
 * @constructor Creates a new Gui
 */
class Gui(private val inputManager: InputManager, fontResourceString: String) {
    private val log = KotlinLogging.logger {}
    
    val context: NkContext
    private val commandBuffer: NkBuffer
    private val nullTexture: NkDrawNullTexture
    private val program: ShaderProgram
    val smallFont: NkFont
    private val mainFont: NkFont
    val bigFont: NkFont
    private val vao: VertexArrayObject
    private val vbo: VertexBufferObject
    private val ebo: ElementBufferObject
    
    
    private val nkAllocator: NkAllocator = NkAllocator.create()
            .alloc { _, _, size -> MemoryUtil.nmemAllocChecked(size) }
            .mfree { _, ptr -> MemoryUtil.nmemFree(ptr) }
    private val nkVertexLayout = NkDrawVertexLayoutElement.create(4)
            .position(0).attribute(Nuklear.NK_VERTEX_POSITION).format(Nuklear.NK_FORMAT_FLOAT).offset(0)
            .position(1).attribute(Nuklear.NK_VERTEX_TEXCOORD).format(Nuklear.NK_FORMAT_FLOAT).offset(8)
            .position(2).attribute(Nuklear.NK_VERTEX_COLOR).format(Nuklear.NK_FORMAT_R8G8B8A8).offset(16)
            .position(3).attribute(Nuklear.NK_VERTEX_ATTRIBUTE_COUNT).format(Nuklear.NK_FORMAT_COUNT).offset(0)
            .flip()
    
    private fun setStyle() {
        context.style().also { style ->
            style.button { button ->
                button.normal().data().color().a(0)
                button.hover().data().color().a(0)
                button.active().data().color().a(0)
                button.border(0f)
                button.text_normal().set(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
                button.text_hover().set(127, 127, 127, 255.toByte())
            }
            
            style.text().color().set(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
        }
    }
    
    init {
        log.debug { "Setting up GUI context" }
        context = NkContext.create()
        commandBuffer = NkBuffer.create()
        nullTexture = NkDrawNullTexture.create()
        
        log.trace { "Setting up Nuklear event handlers" }
        inputManager.apply {
            keyCallbacks.add(priority = 0) { key, _, action, _ ->
                val press = action == GLFW.GLFW_PRESS
                when (key) {
                    GLFW.GLFW_KEY_DELETE                                    -> Nuklear.nk_input_key(context,
                                                                                                    Nuklear.NK_KEY_DEL,
                                                                                                    press)
                    GLFW.GLFW_KEY_ENTER                                     -> Nuklear.nk_input_key(context,
                                                                                                    Nuklear.NK_KEY_ENTER,
                                                                                                    press)
                    GLFW.GLFW_KEY_TAB                                       -> Nuklear.nk_input_key(context,
                                                                                                    Nuklear.NK_KEY_TAB,
                                                                                                    press)
                    GLFW.GLFW_KEY_BACKSPACE                                 -> Nuklear.nk_input_key(context,
                                                                                                    Nuklear.NK_KEY_BACKSPACE,
                                                                                                    press)
                    GLFW.GLFW_KEY_UP                                        -> Nuklear.nk_input_key(context,
                                                                                                    Nuklear.NK_KEY_UP,
                                                                                                    press)
                    GLFW.GLFW_KEY_DOWN                                      -> Nuklear.nk_input_key(context,
                                                                                                    Nuklear.NK_KEY_DOWN,
                                                                                                    press)
                    GLFW.GLFW_KEY_HOME                                      -> {
                        Nuklear.nk_input_key(context, Nuklear.NK_KEY_TEXT_START, press)
                        Nuklear.nk_input_key(context, Nuklear.NK_KEY_SCROLL_START, press)
                    }
                    GLFW.GLFW_KEY_END                                       -> {
                        Nuklear.nk_input_key(context, Nuklear.NK_KEY_TEXT_END, press)
                        Nuklear.nk_input_key(context, Nuklear.NK_KEY_SCROLL_END, press)
                    }
                    GLFW.GLFW_KEY_PAGE_DOWN                                 -> Nuklear.nk_input_key(context,
                                                                                                    Nuklear.NK_KEY_SCROLL_DOWN,
                                                                                                    press)
                    GLFW.GLFW_KEY_PAGE_UP                                   -> Nuklear.nk_input_key(context,
                                                                                                    Nuklear.NK_KEY_SCROLL_UP,
                                                                                                    press)
                    GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT     -> Nuklear.nk_input_key(context,
                                                                                                    Nuklear.NK_KEY_SHIFT,
                                                                                                    press)
                    GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> {
                        if (press) {
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_COPY, isKeyPressed(GLFW.GLFW_KEY_C))
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_PASTE, isKeyPressed(GLFW.GLFW_KEY_P))
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_CUT, isKeyPressed(GLFW.GLFW_KEY_X))
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_TEXT_UNDO, isKeyPressed(GLFW.GLFW_KEY_Z))
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_TEXT_REDO, isKeyPressed(GLFW.GLFW_KEY_R))
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_TEXT_WORD_LEFT,
                                                 isKeyPressed(GLFW.GLFW_KEY_LEFT))
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_TEXT_WORD_RIGHT,
                                                 isKeyPressed(GLFW.GLFW_KEY_RIGHT))
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_TEXT_LINE_START, isKeyPressed(GLFW.GLFW_KEY_B))
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_TEXT_LINE_END, isKeyPressed(GLFW.GLFW_KEY_E))
                        } else {
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_LEFT, isKeyPressed(GLFW.GLFW_KEY_LEFT))
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_RIGHT, isKeyPressed(GLFW.GLFW_KEY_RIGHT))
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_COPY, false)
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_PASTE, false)
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_CUT, false)
                            Nuklear.nk_input_key(context, Nuklear.NK_KEY_SHIFT, false)
                        }
                    }
                }
                false
            }
            mouseButtonCallbacks.add(priority = 0) { button, action, _, xpos, ypos ->
                Nuklear.nk_input_button(context, when (button) {
                    GLFW.GLFW_MOUSE_BUTTON_RIGHT  -> Nuklear.NK_BUTTON_RIGHT
                    GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> Nuklear.NK_BUTTON_MIDDLE
                    else                          -> Nuklear.NK_BUTTON_LEFT
                }, xpos.toInt(), ypos.toInt(), action == GLFW.GLFW_PRESS)
                false
            }
            mouseMoveCallbacks.add(priority = 0) { x, y ->
                Nuklear.nk_input_motion(context, x.toInt(), y.toInt())
                false
            }
            scrollCallbacks.add(priority = 0) { x, y ->
                MemoryStack.stackPush().use { stack ->
                    val scroll = NkVec2.mallocStack(stack)
                            .x(x.toFloat())
                            .y(y.toFloat())
                    Nuklear.nk_input_scroll(context, scroll)
                }
                false
            }
            characterCallbacks.add(priority = 0) { codePoint ->
                Nuklear.nk_input_unicode(context, codePoint)
                false
            }
            
        }
        
        log.trace { "Initializing Nuklear" }
        Nuklear.nk_init(context, nkAllocator, null)
        
        log.trace { "Initializing Nuklear Clipboard" }
        context.clip().also {
            it.copy { _, text, len ->
                if (len == 0) return@copy
                MemoryStack.stackPush().use { stack ->
                    val str = stack.malloc(len + 1)
                    MemoryUtil.memCopy(text, MemoryUtil.memAddress(str), len.toLong())
                    str.put(len, 0.toByte())
                    
                    inputManager.setClipboardString(str)
                }
            }.paste { _, edit ->
                val text = inputManager.getClipboardString()
                if (text != MemoryUtil.NULL) {
                    Nuklear.nnk_textedit_paste(edit, text, Nuklear.nnk_strlen(text))
                }
            }
        }
        
        log.trace { "Initializing GUI Rendering Shaders" }
        val shaderVersion = if (Platform.get() === Platform.MACOSX) "#version 150\n" else "#version 300 es\n"
        val vertexShader = shaderVersion +
                           "uniform mat4 ProjMtx;\n" +
                           "in vec2 Position;\n" +
                           "in vec2 TexCoord;\n" +
                           "in vec4 Color;\n" +
                           "out vec2 Frag_UV;\n" +
                           "out vec4 Frag_Color;\n" +
                           "void main() {\n" +
                           "   Frag_UV = TexCoord;\n" +
                           "   Frag_Color = Color;\n" +
                           "   gl_Position = ProjMtx * vec4(Position.xy, 0, 1);\n" +
                           "}\n"
        val fragmentShader = shaderVersion +
                             "precision mediump float;\n" +
                             "uniform sampler2D Texture;\n" +
                             "in vec2 Frag_UV;\n" +
                             "in vec4 Frag_Color;\n" +
                             "out vec4 Out_Color;\n" +
                             "void main(){\n" +
                             "   Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" +
                             "}\n"
        
        val initialBufferSize = (4 * 1024).toLong()
        
        Nuklear.nk_buffer_init(commandBuffer, nkAllocator, initialBufferSize)
        
        program = ShaderProgram()
        program.createVertexShader(vertexShader)
        program.createFragmentShader(fragmentShader)
        program.link()
        
        log.trace { "Initializing GUI Rendering Buffers" }
        vbo = VertexBufferObject(GL20.glGenBuffers())
        ebo = ElementBufferObject(GL20.glGenBuffers())
        vao = VertexArrayObject(GL30.glGenVertexArrays())
        vao.bound {
            vbo.bound {
                ebo.bound {
                    GL20C.glEnableVertexAttribArray(program.getAttribute("Position"))
                    GL20C.glEnableVertexAttribArray(program.getAttribute("TexCoord"))
                    GL20C.glEnableVertexAttribArray(program.getAttribute("Color"))

                    GL20C.glVertexAttribPointer(program.getAttribute("Position"), 2, GL11C.GL_FLOAT, false, 20, 0)
                    GL20C.glVertexAttribPointer(program.getAttribute("TexCoord"), 2, GL11C.GL_FLOAT, false, 20, 8)
                    GL20C.glVertexAttribPointer(program.getAttribute("Color"), 4, GL11C.GL_UNSIGNED_BYTE, true, 20, 16)
                    program.validate()

                    log.trace { "Initializing Null Texture" }
                    val nullTex = Texture2D(GL11.glGenTextures())
                    nullTexture.texture().id(nullTex.id)
                    nullTexture.uv().set(0.5f, 0.5f)
                    nullTex.bound {
                        MemoryStack.stackPush().use { stack ->
                            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, 1, 1, 0, GL11C.GL_RGBA,
                                               GL12C.GL_UNSIGNED_INT_8_8_8_8_REV, stack.ints(-0x1))
                        }
                        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST)
                        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST)
                    }
                }
            }
        }

        log.trace { "Initializing Nuklear Font" }
        val ttf = IOUtil.readResourceToBuffer(fontResourceString, 512 * 1024)
        smallFont = NkFont.fromTTF(12f, ttf)
        mainFont = NkFont.fromTTF(18f, ttf)
        bigFont = NkFont.fromTTF(36f, ttf)
        Nuklear.nk_style_set_font(context, mainFont.nkFont)
        setStyle()
    }
    
    fun doRegisterInputEvents() {
        Nuklear.nk_input_begin(context)
        
        inputManager.checkForInputEvents()
        val mouse = context.input().mouse()
        
        when {
            mouse.grab()    -> inputManager.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN)
            mouse.grabbed() -> {
                val prevX = mouse.prev().x()
                val prevY = mouse.prev().y()
                inputManager.setCursorPosition(prevX.toDouble(), prevY.toDouble())
                mouse.pos().x(prevX)
                mouse.pos().y(prevY)
            }
            mouse.ungrab()  -> inputManager.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
        }
        
        Nuklear.nk_input_end(context)
    }
    
    fun render(AA: Int, max_vertex_buffer: Int, max_element_buffer: Int, width: Int, height: Int, fWidth: Int,
               fHeight: Int) {
        val blend = GL11C.glIsEnabled(GL11C.GL_BLEND)
        val cull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE)
        val depth = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST)
        val scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST)
        MemoryStack.stackPush().use { stack ->
            // setup global state
            GL11C.glEnable(GL11C.GL_BLEND)
            GL14C.glBlendEquation(GL14C.GL_FUNC_ADD)
            GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA)
            GL11C.glDisable(GL11C.GL_CULL_FACE)
            GL11C.glDisable(GL11C.GL_DEPTH_TEST)
            GL11C.glEnable(GL11C.GL_SCISSOR_TEST)
            
            // setup program
            program.bind()
            GL20C.glUniform1i(program.getUniform("Texture"), 0)
            GL20C.glUniformMatrix4fv(program.getUniform("ProjMtx"), false, stack.floats(
                    2.0f / width, 0.0f, 0.0f, 0.0f,
                    0.0f, -2.0f / height, 0.0f, 0.0f,
                    0.0f, 0.0f, -1.0f, 0.0f,
                    -1.0f, 1.0f, 0.0f, 1.0f
            ))
            GL11C.glViewport(0, 0, fWidth, fHeight)
        }
        
        
        // convert from command queue into draw list and draw to screen
        
        // allocate vertex and element buffer
        vao.bound {
            vbo.bound {
                ebo.bound {
                    GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, max_vertex_buffer.toLong(), GL15C.GL_STREAM_DRAW)
                    GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, max_element_buffer.toLong(), GL15C.GL_STREAM_DRAW)

                    // load draw vertices & elements directly into vertex + element buffer
                    val vertices = Objects.requireNonNull<ByteBuffer>(
                        GL15C.glMapBuffer(GL15C.GL_ARRAY_BUFFER, GL15C.GL_WRITE_ONLY, max_vertex_buffer.toLong(), null)
                    )
                    val elements = Objects.requireNonNull<ByteBuffer>(
                        GL15C.glMapBuffer(
                            GL15C.GL_ELEMENT_ARRAY_BUFFER, GL15C.GL_WRITE_ONLY, max_element_buffer.toLong(),
                            null
                        )
                    )
                    MemoryStack.stackPush().use { stack ->
                        // fill convert configuration
                        val config = NkConvertConfig.callocStack(stack)
                            .vertex_layout(nkVertexLayout)
                            .vertex_size(20)
                            .vertex_alignment(4)
                            .null_texture(nullTexture)
                            .circle_segment_count(22)
                            .curve_segment_count(22)
                            .arc_segment_count(22)
                            .global_alpha(1.0f)
                            .shape_AA(AA)
                            .line_AA(AA)

                        // setup buffers to load vertices and elements
                        val vbuf = NkBuffer.mallocStack(stack)
                        val ebuf = NkBuffer.mallocStack(stack)

                        Nuklear.nk_buffer_init_fixed(vbuf, vertices/*, max_vertex_buffer*/)
                        Nuklear.nk_buffer_init_fixed(ebuf, elements/*, max_element_buffer*/)
                        Nuklear.nk_convert(context, commandBuffer, vbuf, ebuf, config)
                    }
                    GL15C.glUnmapBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER)
                    GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER)

                    // iterate over and execute each draw command
                    val fb_scale_x = fWidth.toFloat() / width.toFloat()
                    val fb_scale_y = fHeight.toFloat() / height.toFloat()

                    var offset = MemoryUtil.NULL
                    var cmd = Nuklear.nk__draw_begin(context, commandBuffer)
                    while (cmd != null) {
                        if (cmd.elem_count() == 0) {
                            cmd = Nuklear.nk__draw_next(cmd, commandBuffer, context)
                            continue
                        }
                        GL13C.glActiveTexture(GL13C.GL_TEXTURE0)
                        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, cmd.texture().id())
                        GL11C.glScissor(
                            (cmd.clip_rect().x() * fb_scale_x).toInt(),
                            ((height - (cmd.clip_rect().y() + cmd.clip_rect().h()).toInt()) * fb_scale_y).toInt(),
                            (cmd.clip_rect().w() * fb_scale_x).toInt(),
                            (cmd.clip_rect().h() * fb_scale_y).toInt()
                        )
                        GL11C.glDrawElements(GL11C.GL_TRIANGLES, cmd.elem_count(), GL11C.GL_UNSIGNED_SHORT, offset)
                        offset += (cmd.elem_count() * 2).toLong()
                        cmd = Nuklear.nk__draw_next(cmd, commandBuffer, context)
                    }
                    Nuklear.nk_clear(context)
                }
            }
        }
        
        // default OpenGL state
        program.unbind()
        if (blend) {
            GL11C.glEnable(GL11C.GL_BLEND)
        } else {
            GL11C.glDisable(GL11C.GL_BLEND)
        }
        if (cull) {
            GL11C.glEnable(GL11C.GL_CULL_FACE)
        } else {
            GL11C.glDisable(GL11C.GL_CULL_FACE)
        }
        if (depth) {
            GL11C.glEnable(GL11C.GL_DEPTH_TEST)
        } else {
            GL11C.glDisable(GL11C.GL_DEPTH_TEST)
        }
        if (scissor) {
            GL11C.glEnable(GL11C.GL_SCISSOR_TEST)
        } else {
            GL11C.glDisable(GL11C.GL_SCISSOR_TEST)
        }
    }
    
    fun cleanup() {
        context.clip().copy()?.free()
        context.clip().paste()?.free()
        Nuklear.nk_free(context)
        
        program.cleanup()
        Texture2D(nullTexture.texture().id()).cleanup()
        vbo.cleanup()
        ebo.cleanup()
        vao.cleanup()
        Nuklear.nk_buffer_free(commandBuffer)
    
        mainFont.cleanup()
        bigFont.cleanup()
        smallFont.cleanup()
        nkAllocator.alloc()?.free()
        nkAllocator.mfree()?.free()
    }
}