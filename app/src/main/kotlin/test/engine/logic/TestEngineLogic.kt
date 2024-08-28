package test.engine.logic

import sp.kx.lwjgl.engine.Engine
import sp.kx.lwjgl.engine.EngineInputCallback
import sp.kx.lwjgl.engine.EngineLogic
import sp.kx.lwjgl.engine.input.Keyboard
import sp.kx.lwjgl.entity.Canvas
import sp.kx.lwjgl.entity.input.KeyboardButton
import sp.kx.math.MutableOffset
import sp.kx.math.MutablePoint
import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.Vector
import sp.kx.math.angleOf
import sp.kx.math.distanceOf
import sp.kx.math.getShortestPoint
import sp.kx.math.isEmpty
import sp.kx.math.lt
import sp.kx.math.measure.MutableDeviation
import sp.kx.math.measure.MutableDoubleMeasure
import sp.kx.math.measure.MutableSpeed
import sp.kx.math.measure.diff
import sp.kx.math.measure.speedOf
import sp.kx.math.moved
import sp.kx.math.plus
import sp.kx.math.pointOf
import sp.kx.math.radians
import test.engine.logic.entity.Barrier
import test.engine.logic.entity.Condition
import test.engine.logic.entity.Crate
import test.engine.logic.entity.Item
import test.engine.logic.entity.Lock
import test.engine.logic.entity.MutableMoving
import test.engine.logic.entity.MutableTurning
import test.engine.logic.entity.Player
import test.engine.logic.entity.Relay
import test.engine.logic.util.closerThan
import test.engine.logic.util.toVectors
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class TestEngineLogic(private val engine: Engine) : EngineLogic {
    private val env = getEnvironment()
    private val interactions = Interactions(env = env)
    private val renders = Renders(engine = engine, env = env)

    private lateinit var shouldEngineStopUnit: Unit

    private val measure = MutableDoubleMeasure(24.0)

    override val inputCallback = object : EngineInputCallback {
        override fun onKeyboardButton(button: KeyboardButton, isPressed: Boolean) {
            when (button) {
                KeyboardButton.P -> {
                    if (isPressed) when (measure.magnitude) {
                        16.0 -> measure.magnitude = 24.0
                        24.0 -> measure.magnitude = 32.0
                        32.0 -> measure.magnitude = 40.0
                        40.0 -> measure.magnitude = 16.0
                    }
                    return
                }
                KeyboardButton.ESCAPE -> {
                    if (env.state == Environment.State.Walking) {
                        if (isPressed) shouldEngineStopUnit = Unit
                        return
                    }
                }
                else -> Unit
            }
            if (isPressed) interactions.onPress(button)
        }
    }

    private fun getCorrectedPoint(
        minDistance: Double,
        target: Point,
        vector: Vector,
    ): Point {
        val point = vector.getShortestPoint(target = target)
        val angle = angleOf(a = point, b = target)
        return point.moved(length = minDistance, angle = angle)
    }

    private fun getCorrectedPoint(
        minDistance: Double,
        target: Point,
        point: Point,
    ): Point {
        val angle = angleOf(a = point, b = target)
        return point.moved(length = minDistance, angle = angle)
    }

    private fun getFinalPoint(
        player: Player,
        minDistance: Double,
        target: Point,
        vectors: List<Vector>,
        points: List<Point>,
    ): Point? {
        val targetDistance = distanceOf(player.moving.point, target)
        // player   target   vector   min
        // |        |        |        |
        // *--------*--------*--------*
        val nearest = vectors.filter { vector ->
            vector.closerThan(point = player.moving.point, minDistance = targetDistance + minDistance)
        }
        val anyCloser = nearest.closerThan(point = target, minDistance = minDistance)
        val conflictPoints = points.filter { point ->
            distanceOf(point, target).lt(other = minDistance, points = 12)
        }
        if (!anyCloser && conflictPoints.isEmpty()) return target
        val correctedPoints = nearest.map { vector ->
            getCorrectedPoint(
                minDistance = minDistance,
                target = target,
                vector = vector,
            )
        } + conflictPoints.map { point ->
            getCorrectedPoint(
                minDistance = minDistance,
                target = target,
                point = point,
            )
        }
        val allowedPoints = correctedPoints.filter { point ->
            !nearest.closerThan(point = point, minDistance = minDistance) &&
                points.none { distanceOf(it, point).lt(other = minDistance, points = 12) }
        }
        if (allowedPoints.isEmpty()) {
            println("[$TAG]: No allowed point!") // todo
            return null // todo
        }
        return allowedPoints.maxByOrNull { point ->
            distanceOf(player.moving.point, point)
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
        val barriers = env.barriers.filter { barrier ->
            !barrier.opened
        }.map { it.vector }
        val finalPoint = getFinalPoint(
            player = env.player,
            minDistance = 1.0,
            target = target,
            vectors = env.walls + barriers,
            points = env.relays.map { it.point } + env.crates.map { it.point },
        ) ?: return
        env.player.moving.point.set(finalPoint)
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

    override fun onRender(canvas: Canvas) {
        if (env.state == Environment.State.Walking) {
            movePlayer()
            if (env.isCameraFree()) moveCamera()
        }
        renders.onRender(canvas = canvas, measure = measure)
    }

    override fun shouldEngineStop(): Boolean {
        return ::shouldEngineStopUnit.isInitialized
    }

    companion object {
        const val TAG = "TestEngineLogic"

        private fun getEnvironment(): Environment {
            val walls = listOf(
                //
                pointOf(-7, 1),
                //
                pointOf(-6, 2),
                //
                pointOf(-5, 2),
                pointOf(-5, 2 + 4),
                pointOf(-5 + 4, 2 + 4),
                pointOf(-5 + 4, 2),
                //
                pointOf(1, 2),
                pointOf(1, 2 + 4),
                pointOf(1 + 4, 2 + 4),
                pointOf(1 + 4, 2),
                //
                pointOf(6, 2),
                //
                pointOf(7, 1),
                pointOf(10, 4),
                pointOf(13, 1),
                pointOf(10, -2),
                //
                pointOf(11, -3),
                //
                pointOf(11, -4),
                pointOf(11 + 4, -4),
                pointOf(11 + 4, -4 - 4),
                pointOf(11, -4 - 4),
                //
                pointOf(11, -10),
                pointOf(11 + 4, -10),
                pointOf(11 + 4, -10 - 4),
                pointOf(11, -10 - 4),
                //
                pointOf(11, -15),
                //
                pointOf(10, -16),
                pointOf(13, -19),
                pointOf(10, -22),
                pointOf(7, -19),
                //
                pointOf(6, -20),
                //
                pointOf(5, -20),
                pointOf(5, -20 - 4),
                pointOf(5 - 4, -20 - 4),
                pointOf(5 - 4, -20),
                //a
                pointOf(-1, -20),
                pointOf(-1, -20 - 4),
                pointOf(-1 - 4, -20 - 4),
                pointOf(-1 - 4, -20),
                //
                pointOf(-6, -20),
                //
                pointOf(-7, -19),
                pointOf(-7, 1), // todo
            ).toVectors()
            val player = Player(
                id = UUID(1_000_001, 1),
                moving = MutableMoving(
                    point = MutablePoint(x = 0.0, y = -8.0),
                    speed = MutableSpeed(magnitude = 8.0, timeUnit = TimeUnit.SECONDS),
                ),
                turning = MutableTurning(
                    direction = MutableDeviation(actual = 0.0, expected = 0.0),
                    directionSpeed = speedOf(kotlin.math.PI * 2),
                ),
            )
            val camera = MutableMoving(
                point = MutablePoint(x = 0.0, y = 0.0),
                speed = MutableSpeed(magnitude = 12.0, timeUnit = TimeUnit.SECONDS),
            )
            val conditions = listOf(
                Condition(
                    id = UUID(1, 1),
                    depends = emptyList(),
                    tags = listOf(
                        setOf(UUID(0, 1)),
                    ),
                ),
                Condition(
                    id = UUID(1, 2),
                    depends = emptyList(),
                    tags = listOf(
                        setOf(UUID(0, 1), UUID(0, 2)),
                    ),
                ),
                Condition(
                    id = UUID(1, 3),
                    depends = emptyList(),
                    tags = listOf(
                        setOf(UUID(0, 1), UUID(0, 3)),
                        setOf(UUID(0, 2), UUID(0, 3)),
                    ),
                ),
            )
            val barriers = listOf(
                Barrier(
                    vector = pointOf(x = -5, y = 2) + pointOf(x = -1, y = 2),
                    opened = false,
                    conditions = emptyList(),
                ),
                Barrier(
                    vector = pointOf(x = 1, y = 2) + pointOf(x = 5, y = 2),
                    opened = false,
                    conditions = listOf(
                        setOf(UUID(1, 1)),
                    ),
                ),
                Barrier(
                    vector = pointOf(x = 7, y = 1) + pointOf(x = 10, y = -2),
                    opened = false,
                    conditions = listOf(
                        setOf(UUID(1, 2)),
                    ),
                ),
                Barrier(
                    vector = pointOf(x = 11, y = -4) + pointOf(x = 11, y = -8),
                    opened = false,
                    conditions = listOf(
                        setOf(UUID(1, 3)),
                    ),
                ),
//                Barrier(
//                    vector = pointOf(x = 11, y = -10) + pointOf(x = 11, y = -14),
//                    opened = false,
//                    conditions = emptyList(),
//                ),
            )
            val relays = listOf(
                Relay(
                    point = pointOf(-3, 5),
                    enabled = false,
                    tags = setOf(UUID(0, 1)),
                ),
                Relay(
                    point = pointOf(3, 5),
                    enabled = false,
                    tags = setOf(UUID(0, 2)),
                ),
                Relay(
                    point = pointOf(10, 2),
                    enabled = false,
                    tags = setOf(UUID(0, 3)),
                ),
            )
            val items = listOf(
                Item(
                    id = UUID(0x0000100000000000, 1),
                    tags = setOf(UUID(1_200_001, 1)),
                    point = MutablePoint(0.0, 0.0),
                    owner = null,
                ),
                Item(
                    id = UUID(0x0001100000000000, 1),
                    tags = emptySet(), // todo
                    point = MutablePoint(0.0, -4.0),
                    owner = null,
                ),
            )
            val crates = listOf(
                Crate(
                    id = UUID(0x0000200000000000, 1),
                    point = pointOf(-2, -8),
                    lock = Lock(
                        opened = true,
                        required = emptyList(),
                    ),
                ),
                Crate(
                    id = UUID(0x0001200000000000, 1),
                    point = pointOf(-2, -12),
                    lock = Lock(
                        opened = false,
                        required = listOf(
                            setOf(UUID(1_200_001, 1)),
                        ),
                    ),
                ),
            )
            return Environment(
                state = Environment.State.Walking,
                walls = walls,
                player = player,
                camera = camera,
                isCameraFree = false,
                conditions = conditions,
                barriers = barriers,
                relays = relays,
                items = items,
                crates = crates,
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
