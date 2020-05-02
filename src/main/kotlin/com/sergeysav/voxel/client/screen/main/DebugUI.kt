package com.sergeysav.voxel.client.screen.main

import com.sergeysav.voxel.client.nuklear.Gui
import com.sergeysav.voxel.client.nuklear.GuiWindow
import org.lwjgl.nuklear.Nuklear

/**
 * @author sergeys
 *
 * @constructor Creates a new DebugUI
 */
class DebugUI : GuiWindow("Debug") {

    private var runningIndex = 0
    private val runningAverage = DoubleArray(30) { 0.0 }

    fun layout(gui: Gui, fps: Double, meshingQueue: Int, loadingQueue: Int) {
        runningAverage[runningIndex++] = fps
        runningIndex %= runningAverage.size

        nkEditing(gui) {
            window.background.hidden {
                window(0f, 0f, 200f, 20*4f,
                    Nuklear.NK_WINDOW_NO_INPUT or
                            Nuklear.NK_WINDOW_ROM or
                            Nuklear.NK_WINDOW_NO_SCROLLBAR) {
                    dynamicRow(1, 15f) {
                        label(String.format("Average FPS: %.1f", runningAverage.average()))
                    }
                    dynamicRow(1, 15f) {
                        label(String.format("Minimum FPS: %.1f", runningAverage.min()))
                    }
                    dynamicRow(1, 15f) {
                        label(String.format("Meshing Queue: %d", meshingQueue))
                    }
                    dynamicRow(1, 15f) {
                        label(String.format("Loading Queue: %d", loadingQueue))
                    }
                }
            }
        }
    }
}