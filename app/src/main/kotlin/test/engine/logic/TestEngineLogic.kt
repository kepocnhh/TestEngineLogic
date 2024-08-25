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
import sp.kx.math.center
import sp.kx.math.centerPoint
import sp.kx.math.dby
import sp.kx.math.distanceOf
import sp.kx.math.isEmpty
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
import sp.kx.math.offsetOf
import sp.kx.math.plus
import sp.kx.math.pointOf
import sp.kx.math.radians
import sp.kx.math.vectorOf
import test.engine.logic.entity.MutableMoving
import test.engine.logic.entity.MutableTurning
import test.engine.logic.util.FontInfoUtil
import test.engine.logic.util.minus
import test.engine.logic.util.plus
import test.engine.logic.util.toVectors
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

internal class TestEngineLogic(private val engine: Engine) : EngineLogic {
    private class Player(
        val moving: MutableMoving,
        val turning: MutableTurning,
    )

    private class Environment(
        val walls: List<Vector>,
        val player: Player,
        val camera: MutableMoving,
        private var isCameraFree: Boolean,
    ) {
        fun isCameraFree(): Boolean {
            return isCameraFree
        }

        fun switchCamera() {
            camera.point.set(player.moving.point)
            isCameraFree = !isCameraFree
        }
    }

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
                KeyboardButton.C -> {
                    if (!isPressed) env.switchCamera()
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
        canvas.vectors.draw(
            color = Color.WHITE,
            vector = vectorOf(
                startX = 0.0,
                startY = pictureSize.height / 2,
                finishX = 2.0,
                finishY = pictureSize.height / 2,
            ),
            lineWidth = 0.1,
            measure = measure,
        )
        canvas.vectors.draw(
            color = Color.GREEN,
            vector = vectorOf(
                startX = 1.0,
                startY = 0.0,
                finishX = 1.0,
                finishY = pictureSize.height,
            ),
            lineWidth = 0.1,
            measure = measure,
        )
        val info = FontInfoUtil.getFontInfo(height = 0.75, measure = measure)
        val xHalf = pictureSize.width.toInt() / 2
        val xNumber = kotlin.math.ceil(point.x).toInt()
        val xNumbers = (xNumber - xHalf - 2)..(xNumber + xHalf)
        for (x in xNumbers) {
            val textY = if (x % 2 == 0) 1.0 else 0.25
            canvas.texts.draw(
                color = Color.GREEN,
                info = info,
                pointTopLeft = pointOf(x = x + offset.dX + 0.25, y = textY),
                measure = measure,
                text = String.format("%2d", x),
            )
            val lineY = if (x % 2 == 0) 1.5 else 0.5
            canvas.vectors.draw(
                color = Color.GREEN,
                vector = pointOf(x = x + offset.dX, y = 1.0) + pointOf(x = x + offset.dX, y = lineY),
                lineWidth = 0.1,
                measure = measure,
            )
        }
        val yHalf = pictureSize.height.toInt() / 2
        val yNumber = kotlin.math.ceil(point.y).toInt()
        val yNumbers = (yNumber - yHalf + 2)..(yNumber + yHalf)
        for (y in yNumbers) {
            val textX = if (y % 2 == 0) 1.25 else 1.75
            canvas.texts.draw(
                color = Color.GREEN,
                info = info,
                pointTopLeft = pointOf(x = textX, y = y + offset.dY),
                measure = measure,
                text = String.format("%2d", y),
            )
            val lineX = if (y % 2 == 0) 0.5 else 1.5
            canvas.vectors.draw(
                color = Color.GREEN,
                vector = pointOf(x = 1.0, y = y + offset.dY) + pointOf(x = lineX, y = y + offset.dY),
                lineWidth = 0.1,
                measure = measure,
            )
        }
    }

    private fun movePlayer() {
        val offset = engine.input.keyboard.getOffset(
            upKey = KeyboardButton.W,
            downKey = KeyboardButton.S,
            leftKey = KeyboardButton.A,
            rightKey = KeyboardButton.D,
        )
        if (offset.isEmpty()) return
        val timeDiff = engine.property.time.diff()
        env.player.turning.turn(
            radians = angleOf(offset).radians(),
            timeDiff = timeDiff,
        )
        val length = env.player.moving.speed.length(timeDiff)
        val multiplier = kotlin.math.min(1.0, distanceOf(offset))
        val target = env.player.moving.point.moved(
            length = length * multiplier,
            angle = env.player.turning.direction.expected,
        )
        env.player.moving.point.set(target)
    }

    private fun moveCamera() {
        val offset = engine.input.keyboard.getOffset(
            upKey = KeyboardButton.UP,
            downKey = KeyboardButton.DOWN,
            leftKey = KeyboardButton.LEFT,
            rightKey = KeyboardButton.RIGHT,
        )
        if (offset.isEmpty()) return
        val timeDiff = engine.property.time.diff()
        val length = env.camera.speed.length(timeDiff)
        val multiplier = kotlin.math.min(1.0, distanceOf(offset))
        val target = env.camera.point.moved(
            length = length * multiplier,
            angle = angleOf(offset).radians(),
        )
        env.camera.point.set(target)
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
                x = 4.0,
                y = 2.0 + y++,
            ),
            text = String.format(
                "Picture: %.1fx%.1f (%.2fx%.2f)",
                engine.property.pictureSize.width,
                engine.property.pictureSize.height,
                pictureSize.width,
                pictureSize.height,
            ),
            measure = measure,
        )
        val point = env.player.moving.point
        canvas.texts.draw(
            color = Color.GREEN,
            info = info,
            pointTopLeft = pointOf(
                x = 4.0,
                y = 2.0 + y++,
            ),
            text = String.format("Player: {x: %.2f, y: %.2f}", point.x, point.y),
            measure = measure,
        )
    }

    private fun onRenderPlayer(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        canvas.vectors.draw(
            color = Color.WHITE,
            vector = vectorOf(env.player.moving.point, length = 1.0, angle = env.player.turning.direction.actual),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
    }

    private fun onRenderCamera(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        canvas.vectors.draw(
            color = Color.GREEN,
            vector = vectorOf(-1.0, 0.0, 1.0, 0.0),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
        canvas.vectors.draw(
            color = Color.GREEN,
            vector = vectorOf(0.0, -1.0, 0.0, 1.0),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
        val info = FontInfoUtil.getFontInfo(height = 0.75, measure = measure)
        canvas.texts.draw(
            color = Color.GREEN,
            info = info,
            pointTopLeft = Point.Center.moved(0.5),
            text = String.format("x: %.2f y: %.2f", env.camera.point.x, env.camera.point.y),
            offset = offset,
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
        movePlayer() // todo
        if (env.isCameraFree()) {
            moveCamera()
        }
        //
        val pictureSize = engine.property.pictureSize - measure
        val centerPoint = engine.property.pictureSize.centerPoint() - measure
        val centerOffset = engine.property.pictureSize.center() - measure
        val point = if (env.isCameraFree()) {
            env.camera.point
        } else {
            env.player.moving.point
        }
        val offset = centerPoint - point
        //
        canvas.vectors.draw(
            color = Color.GRAY,
            vectors = env.walls,
            offset = offset,
            measure = measure,
        )
        onRenderPlayer(
            canvas = canvas,
            offset = if (env.isCameraFree()) {
                offset
            } else {
                centerPoint - env.player.moving.point
            },
            measure = measure,
        )
        if (env.isCameraFree()) {
            onRenderCamera(
                canvas = canvas,
                offset = centerOffset,
                measure = measure,
            )
        }
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
                moving = MutableMoving(
                    point = MutablePoint(x = 0.0, y = 0.0),
                    speed = MutableSpeed(magnitude = 8.0, timeUnit = TimeUnit.SECONDS),
                ),
                turning = MutableTurning(
                    direction = MutableDeviation(actual = 0.0, expected = 0.0),
                    directionSpeed = speedOf(kotlin.math.PI * 2),
                ),
            )
            val camera = MutableMoving(
                point = MutablePoint(x = 0.0, y = 0.0),
                speed = MutableSpeed(magnitude = 8.0, timeUnit = TimeUnit.SECONDS),
            )
            return Environment(
                walls = walls,
                player = player,
                camera = camera,
                isCameraFree = false,
            )
        }

        private fun Keyboard.getOffset(
            upKey: KeyboardButton,
            downKey: KeyboardButton,
            leftKey: KeyboardButton,
            rightKey: KeyboardButton,
        ): Offset {
            val result = MutableOffset(dX = 0.0, dY = 0.0)
            val down = isPressed(downKey)
            if (isPressed(upKey)) {
                if (!down) result.dY = -1.0
            } else if (down) {
                result.dY = 1.0
            }
            val right = isPressed(rightKey)
            if (isPressed(leftKey)) {
                if (!right) result.dX = -1.0
            } else if (right) {
                result.dX = 1.0
            }
            return result
        }
    }
}
