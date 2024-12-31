package test.engine.logic

import sp.kx.lwjgl.engine.Engine
import sp.kx.math.sizeOf

fun main() {
    Engine.run(
        title = "Test",
        supplier = ::TestEngineLogics,
        size = sizeOf(640, 480),
        defaultFontName = "JetBrainsMono.ttf",
    )
}
