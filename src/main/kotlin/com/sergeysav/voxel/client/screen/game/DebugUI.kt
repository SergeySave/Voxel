package com.sergeysav.voxel.client.screen.game

import com.sergeysav.voxel.client.nuklear.Gui
import com.sergeysav.voxel.client.nuklear.GuiWindow
import org.lwjgl.nuklear.Nuklear
import java.lang.StringBuilder
import kotlin.math.roundToInt

/**
 * @author sergeys
 *
 * @constructor Creates a new DebugUI
 */
class DebugUI : GuiWindow("Debug") {

    private var runningIndex = 0
    private val runningAverage = DoubleArray(15) { 0.0 }
    private val stringBuilder = StringBuilder(128)

    fun layout(gui: Gui, fps: Double, meshingQueue: Int, loadingQueue: Int, savingQueue: Int) {
        runningAverage[runningIndex++] = fps
        runningIndex %= runningAverage.size

        nkEditing(gui) {
            window.background.hidden {
                window(0f, 0f, 400f, 20*6f,
                    Nuklear.NK_WINDOW_NO_INPUT or
                            Nuklear.NK_WINDOW_ROM or
                            Nuklear.NK_WINDOW_NO_SCROLLBAR) {
                    dynamicRow(1, 15f) {
                        stringBuilder.clear()
                        stringBuilder.append("Average FPS: ")
                        stringBuilder.append((runningAverage.average() * 10).roundToInt() / 10.0)
                        label(stringBuilder)
                    }
                    dynamicRow(1, 15f) {
                        stringBuilder.clear()
                        stringBuilder.append("Minimum FPS: ")
                        stringBuilder.append((runningAverage.min()!! * 10).roundToInt() / 10.0)
                        label(stringBuilder)
                    }
                    dynamicRow(1, 15f) {
                        stringBuilder.clear()
                        stringBuilder.append("Meshing Queue: ")
                        stringBuilder.append(meshingQueue)
                        label(stringBuilder)
                    }
                    dynamicRow(1, 15f) {
                        stringBuilder.clear()
                        stringBuilder.append("Loading Queue: ")
                        stringBuilder.append(loadingQueue)
                        label(stringBuilder)
                    }
                    dynamicRow(1, 15f) {
                        stringBuilder.clear()
                        stringBuilder.append("Saving Queue: ")
                        stringBuilder.append(savingQueue)
                        label(stringBuilder)
                    }
                    dynamicRow(1, 15f) {
                        stringBuilder.clear()
                        stringBuilder.append("Memory: ")
                        stringBuilder.append((((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0) * 100).roundToInt() / 100.0)
                        stringBuilder.append('/')
                        stringBuilder.append(((Runtime.getRuntime().totalMemory() / 1024.0 / 1024.0) * 100).roundToInt() / 100.0)
                        label(stringBuilder)
                    }
                }
            }
        }
    }
}