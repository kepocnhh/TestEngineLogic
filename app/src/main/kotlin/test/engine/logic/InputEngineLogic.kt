package test.engine.logic

import sp.kx.lwjgl.engine.Engine
import sp.kx.lwjgl.engine.EngineInputCallback
import sp.kx.lwjgl.engine.EngineLogic
import sp.kx.lwjgl.entity.Canvas
import sp.kx.lwjgl.entity.Color
import sp.kx.lwjgl.entity.input.KeyboardButton
import sp.kx.math.Point
import sp.kx.math.measure.frequency
import test.engine.logic.util.FontInfoUtil

internal class InputEngineLogic(private val engine: Engine) : EngineLogic {
    private lateinit var shouldEngineStopUnit: Unit

    override val inputCallback = object : EngineInputCallback {
        override fun onKeyboardButton(button: KeyboardButton, isPressed: Boolean) {
            when (button) {
                KeyboardButton.ESCAPE -> {
                    if (!isPressed) {
                        shouldEngineStopUnit = Unit
                    }
                }
                else -> {
                    println("[InputEngineLogic]: on button: $button $isPressed")
                }
            }
        }
    }

    override fun onRender(canvas: Canvas) {
        val fps = engine.property.time.frequency()
        canvas.texts.draw(
            info = FontInfoUtil.getFontInfo(height = 16f),
            pointTopLeft = Point.Center,
            color = Color.GREEN,
            text = String.format("%.2f", fps),
        )
    }

    override fun shouldEngineStop(): Boolean {
        return ::shouldEngineStopUnit.isInitialized
    }
}
