package test.engine.logic

import sp.kx.lwjgl.engine.Engine
import sp.kx.lwjgl.engine.EngineInputCallback
import sp.kx.lwjgl.engine.EngineLogic
import sp.kx.lwjgl.engine.input.Keyboard
import sp.kx.lwjgl.entity.Canvas
import sp.kx.lwjgl.entity.Color
import sp.kx.lwjgl.entity.input.KeyboardButton
import sp.kx.math.MutableOffset
import sp.kx.math.MutablePoint
import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.Vector
import sp.kx.math.angleOf
import sp.kx.math.centerPoint
import sp.kx.math.copy
import sp.kx.math.distanceOf
import sp.kx.math.isEmpty
import sp.kx.math.measure.Deviation
import sp.kx.math.measure.Measure
import sp.kx.math.measure.MutableDeviation
import sp.kx.math.measure.MutableDoubleMeasure
import sp.kx.math.measure.MutableSpeed
import sp.kx.math.measure.Speed
import sp.kx.math.measure.diff
import sp.kx.math.measure.frequency
import sp.kx.math.measure.speedOf
import sp.kx.math.minus
import sp.kx.math.moved
import sp.kx.math.plus
import sp.kx.math.pointOf
import sp.kx.math.radians
import sp.kx.math.toVector
import sp.kx.math.vectorOf
import test.engine.logic.entity.MutableMoving
import test.engine.logic.entity.MutableTurning
import test.engine.logic.util.FontInfoUtil
import test.engine.logic.util.minus
import test.engine.logic.util.toVectors
import java.util.concurrent.TimeUnit

internal class TestEngineLogic(private val engine: Engine) : EngineLogic {
    private class Player(
        point: Point,
        speed: Speed,
        override val direction: MutableDeviation<Double>,
        override val directionSpeed: Speed,
    ) : MutableMoving, MutableTurning {
        override val point = MutablePoint(x = point.x, y = point.y)
        override val speed = MutableSpeed(magnitude = speed.per(TimeUnit.NANOSECONDS), timeUnit = TimeUnit.NANOSECONDS)
    }

    private class Environment(
        val walls: List<Vector>,
        val player: Player,
    )

    private val env = getEnvironment()

    private lateinit var shouldEngineStopUnit: Unit

    private val measure = MutableDoubleMeasure(24.0)

    override val inputCallback = object : EngineInputCallback {
        override fun onKeyboardButton(button: KeyboardButton, isPressed: Boolean) {
            when (button) {
                KeyboardButton.ESCAPE -> {
                    if (!isPressed) shouldEngineStopUnit = Unit
                }
                KeyboardButton.P -> {
                    if (!isPressed) {
                        when (measure.magnitude) {
                            16.0 -> measure.magnitude = 24.0
                            24.0 -> measure.magnitude = 32.0
                        }
                    }
                }
                KeyboardButton.M -> {
                    if (!isPressed) {
                        when (measure.magnitude) {
                            24.0 -> measure.magnitude = 16.0
                            32.0 -> measure.magnitude = 24.0
                        }
                    }
                }
                else -> {
                    println("[$TAG]: on button: $button $isPressed")
                }
            }
        }
    }

    private fun onRenderGrid(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
        point: Point,
    ) {
        val pictureSize = engine.property.pictureSize - measure
        canvas.vectors.draw(
            color = Color.WHITE,
            vector = vectorOf(
                startX = pictureSize.width / 2,
                startY = 0.0,
                finishX = pictureSize.width / 2,
                finishY = 2.0,
            ),
            lineWidth = 0.1,
            measure = measure,
        )
        canvas.vectors.draw(
            color = Color.GREEN,
            vector = vectorOf(
                startX = 0.0,
                startY = 1.0,
                finishX = pictureSize.width,
                finishY = 1.0,
            ),
            lineWidth = 0.1,
            measure = measure,
        )
        // todo y
        val info = FontInfoUtil.getFontInfo(height = 0.75, measure = measure)
        val xLen = (pictureSize.width.toInt() / 2) * 2 + 2
        val xNumbers = (point.x.toInt() - xLen / 2 - 1)..(point.x.toInt() + xLen / 2 + 1)
        for (x in xNumbers) {
            val textY = if (x % 2 == 0) 1.0 else 0.25
            canvas.texts.draw(
                color = Color.GREEN,
                info = info,
                pointTopLeft = pointOf(x = x + offset.dX, y = textY),
                measure = measure,
                text = String.format("%2d", x),
            )
        }
//        val xLen = measure.units(engine.property.pictureSize.width).toInt() - 6
//        val xNumbers = (point.x.toInt() - xLen / 2)..(point.x.toInt() + xLen / 2)
//        for (x in xNumbers) {
//            val textY = if (x % 2 == 0) 1.0 else 0.25
//            val xOffset = offset.copy(dY = 0.0)
//            canvas.texts.draw(
//                color = Color.GREEN,
//                info = info,
//                pointTopLeft = pointOf(x = x.toDouble(), y = textY),
//                offset = xOffset,
//                measure = measure,
//                text = String.format("%2d", x),
//            )
//            val lineY = if (x % 2 == 0) 1.5 else 0.5
//            canvas.vectors.draw(
//                color = Color.GREEN,
//                vector = pointOf(x = x.toDouble(), y = 1.0) + pointOf(x = x.toDouble(), y = lineY),
//                offset = xOffset,
//                measure = measure,
//            )
//        }
        // todo y
    }

    private fun onWalking() {
        val playerOffset = engine.input.keyboard.getPlayerOffset()
        if (playerOffset.isEmpty()) return
        val timeDiff = engine.property.time.diff()
        env.player.direction.expected = angleOf(playerOffset).radians()
        val dirDiff = env.player.direction.diff() // todo
        val length = env.player.speed.length(timeDiff)
        val multiplier = kotlin.math.min(1.0, distanceOf(playerOffset))
        val target = env.player.point.moved(
            length = length * multiplier,
            angle = env.player.direction.expected,
        )
        env.player.point.set(target)
    }

    private fun onRenderDebug(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        val info = FontInfoUtil.getFontInfo(height = 0.75, measure = measure)
        val pictureSize = engine.property.pictureSize - measure
        var y = 0
        canvas.texts.draw(
            color = Color.GREEN,
            info = info,
            pointTopLeft = pointOf(
                x = 2.0,
                y = 2.0 + y++,
            ),
            text = String.format("Picture size: %.1fx%.1f", engine.property.pictureSize.width, engine.property.pictureSize.height),
            measure = measure,
        )
        canvas.texts.draw(
            color = Color.GREEN,
            info = info,
            pointTopLeft = pointOf(
                x = 2.0,
                y = 2.0 + y++,
            ),
            text = String.format("Picture size(measured): %.2fx%.2f", pictureSize.width, pictureSize.height),
            measure = measure,
        )
        val point = env.player.point
        canvas.texts.draw(
            color = Color.GREEN,
            info = info,
            pointTopLeft = pointOf(
                x = 2.0,
                y = 2.0 + y++,
            ),
            text = String.format("Player point: {x: %.2f, y: %.2f}", point.x, point.y),
            measure = measure,
        )
    }

    override fun onRender(canvas: Canvas) {
        val fps = engine.property.time.frequency()
//        canvas.texts.draw(
//            info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure),
//            pointTopLeft = Point.Center,
//            color = Color.GREEN,
//            text = String.format("%.2f", fps),
//            measure = measure,
//        )
        //
        onWalking() // todo
        //
        val center = pointOf(
            x = measure.units(engine.property.pictureSize.width / 2),
            y = measure.units(engine.property.pictureSize.height / 2),
        )
        val point = env.player.point
        val offset = center - point
        //
        canvas.vectors.draw(
            color = Color.GRAY,
            vectors = env.walls,
            offset = offset,
            measure = measure,
        )
        // todo
        //
        onRenderGrid(
            canvas = canvas,
            offset = offset,
            measure = measure,
            point = point,
        )
        //
        if (engine.input.keyboard.isPressed(KeyboardButton.I)) {
            onRenderDebug(
                canvas = canvas,
                offset = offset,
                measure = measure,
            )
        }
    }

    override fun shouldEngineStop(): Boolean {
        return ::shouldEngineStopUnit.isInitialized
    }

    companion object {
        const val TAG = "TestEngineLogic"

        private fun getEnvironment(): Environment {
            val walls = listOf(
                pointOf(x = 0, y = 0),
                pointOf(x = 1, y = 1),
            ).toVectors()
            val player = Player(
                point = pointOf(x = 0, y = 0),
                speed = speedOf(magnitude = 7.5, timeUnit = TimeUnit.SECONDS),
                direction = MutableDeviation(actual = 0.0, expected = 0.0),
                directionSpeed = speedOf(kotlin.math.PI * 2),
            )
            return Environment(
                walls = walls,
                player = player,
            )
        }

        private fun Keyboard.getPlayerOffset(): Offset {
            val result = MutableOffset(dX = 0.0, dY = 0.0)
            val up = isPressed(KeyboardButton.W) || isPressed(KeyboardButton.UP)
            val down = isPressed(KeyboardButton.S) || isPressed(KeyboardButton.DOWN)
            if (up) {
                if (!down) result.dY = -1.0
            } else if (down) {
                result.dY = 1.0
            }
            val left = isPressed(KeyboardButton.A) || isPressed(KeyboardButton.LEFT)
            val right = isPressed(KeyboardButton.D) || isPressed(KeyboardButton.RIGHT)
            if (left) {
                if (!right) result.dX = -1.0
            } else if (right) {
                result.dX = 1.0
            }
            return result
        }
    }
}
