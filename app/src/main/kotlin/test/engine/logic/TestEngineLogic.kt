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
import sp.kx.math.angle
import sp.kx.math.angleOf
import sp.kx.math.center
import sp.kx.math.centerPoint
import sp.kx.math.dby
import sp.kx.math.distanceOf
import sp.kx.math.getShortestDistance
import sp.kx.math.getShortestPoint
import sp.kx.math.gt
import sp.kx.math.isEmpty
import sp.kx.math.lt
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
import sp.kx.math.sizeOf
import sp.kx.math.times
import sp.kx.math.toOffset
import sp.kx.math.vectorOf
import test.engine.logic.entity.Barrier
import test.engine.logic.entity.Condition
import test.engine.logic.entity.Item
import test.engine.logic.entity.MutableMoving
import test.engine.logic.entity.MutableTurning
import test.engine.logic.entity.Relay
import test.engine.logic.util.FontInfoUtil
import test.engine.logic.util.closerThan
import test.engine.logic.util.diagonal
import test.engine.logic.util.diagonalAngle
import test.engine.logic.util.drawRectangle
import test.engine.logic.util.drawVectors
import test.engine.logic.util.drawCircle
import test.engine.logic.util.minus
import test.engine.logic.util.plus
import test.engine.logic.util.toVectors
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

internal class TestEngineLogic(private val engine: Engine) : EngineLogic {
    private class Player(
        val id: UUID,
        val moving: MutableMoving,
        val turning: MutableTurning,
    )

    private class Environment(
        val walls: List<Vector>,
        val player: Player,
        val camera: MutableMoving,
        private var isCameraFree: Boolean,
        val conditions: List<Condition>,
        val barriers: List<Barrier>,
        val relays: List<Relay>,
        val items: List<Item>,
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

    private fun onInteractionBarrier(barrier: Barrier) {
        barrier.opened = !barrier.opened // todo
    }

    private fun isPassed(condition: Condition): Boolean {
        return (condition.tags.isEmpty() || condition.tags.any { set ->
            set.all { tag ->
                env.relays.any { relay ->
                    relay.tags.contains(tag) && relay.enabled
                }
            }
        }) && (condition.depends.isEmpty() || condition.depends.any { set ->
            set.all { id ->
                isPassed(condition = env.conditions.firstOrNull { it.id == id } ?: TODO())
            }
        })
    }

    private fun onInteractionRelay(relay: Relay) {
        relay.enabled = !relay.enabled
        for (barrier in env.barriers) {
            if (barrier.conditions.isEmpty()) continue
            barrier.opened = barrier.conditions.any { set ->
                set.all { id ->
                    val condition = env.conditions.firstOrNull { it.id == id } ?: TODO()
                    isPassed(condition = condition)
                }
            }
        }
    }

    private fun onInteractionItem(item: Item) {
        item.ownerID = env.player.id
    }

    private fun onInteraction() {
        val barrier = getNearestBarrier(
            target = env.player.moving.point,
            barriers = env.barriers,
            minDistance = 1.0,
            maxDistance = 1.75,
        )
        if (barrier != null) {
            onInteractionBarrier(barrier = barrier)
            return
        }
        val relay = getNearestRelay(
            target = env.player.moving.point,
            relays = env.relays,
            maxDistance = 1.75,
        )
        if (relay != null) {
            onInteractionRelay(relay = relay)
            return
        }
        val item = getNearestItem(
            target = env.player.moving.point,
            items = env.items,
            maxDistance = 1.75,
        )
        if (item != null) {
            onInteractionItem(item = item)
            return
        }
    }

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
                            32.0 -> measure.magnitude = 40.0
                            40.0 -> measure.magnitude = 16.0
                        }
                    }
                }
                KeyboardButton.C -> {
                    if (!isPressed) env.switchCamera()
                }
                KeyboardButton.F -> {
                    if (!isPressed) onInteraction()
                }
                else -> {
                    println("[$TAG]: on button: $button $isPressed") // todo
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
                text = String.format("%d", x),
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
                text = String.format("%d", y),
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
            points = env.relays.map { it.point },
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

    private fun onRenderWalls(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
        walls: List<Vector>,
    ) {
//        drawVectors(
        canvas.vectors.draw(
            color = Color.GRAY,
            vectors = walls,
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
    }

    private fun onRenderBarriers(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
        barriers: List<Barrier>,
    ) {
        val size = sizeOf(0.3, 0.3)
        for (barrier in barriers) {
            if (!barrier.opened) {
                canvas.vectors.draw(
                    color = Color.RED,
                    vector = barrier.vector,
                    offset = offset,
                    measure = measure,
                    lineWidth = 0.2,
                )
            }
            val angle = barrier.vector.angle()
            val startPoint = barrier.vector.start.moved(length = size.diagonal() / 2, angle = angle - kotlin.math.PI / 2 - size.diagonalAngle())
            canvas.polygons.drawRectangle(
                color = Color.YELLOW,
                pointTopLeft = startPoint,
                size = size,
                pointOfRotation = startPoint,
                offset = offset,
                measure = measure,
                direction = angle,
            )
            val finishPoint = barrier.vector.finish.moved(length = size.diagonal() / 2, angle = angle - kotlin.math.PI / 2 - size.diagonalAngle())
            canvas.polygons.drawRectangle(
                color = Color.YELLOW,
                pointTopLeft = finishPoint,
                size = size,
                pointOfRotation = finishPoint,
                direction = angle,
                offset = offset,
                measure = measure,
            )
        }
    }

    private fun onRenderRelays(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
        relays: List<Relay>,
    ) {
        val size = sizeOf(2, 1)
        val info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure)
        for (relay in relays) {
            canvas.polygons.drawRectangle(
                color = if (relay.enabled) Color.GREEN else Color.RED,
                pointTopLeft = relay.point,
                size = size,
                offset = offset + size.center() * -1.0,
                measure = measure,
            )
            val text = if (relay.enabled) "on" else "off"
            val textWidth = engine.fontAgent.getTextWidth(info, text)
            canvas.texts.draw(
                color = Color.BLACK,
                info = info,
                pointTopLeft = relay.point,
                offset = offset + offsetOf(dX = measure.units(textWidth) / 2, dY = size.height / 2) * -1.0,
                measure = measure,
                text = text,
            )
        }
    }

    private fun onRenderPlayer(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        canvas.polygons.drawCircle(
            borderColor = Color.BLUE,
            fillColor = Color.BLUE.copy(alpha = 0.5f),
            pointCenter = env.player.moving.point,
            radius = 1.0,
            edgeCount = 16,
            lineWidth = 0.1,
            offset = offset,
            measure = measure,
        )
        canvas.vectors.draw(
            color = Color.YELLOW,
            vector = vectorOf(env.player.moving.point, length = 1.0, angle = env.player.turning.direction.expected),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
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

    private fun getNearestBarrier(
        target: Point,
        barriers: List<Barrier>,
        minDistance: Double,
        maxDistance: Double,
    ): Barrier? {
        var nearest: Pair<Barrier, Double>? = null
        for (barrier in barriers) {
            if (barrier.conditions.isNotEmpty()) continue // todo
            val distance = barrier.vector.getShortestDistance(target)
            if (distance.lt(other = minDistance, points = 12)) continue
            if (distance.gt(other = maxDistance, points = 12)) continue
            if (nearest == null) {
                nearest = barrier to distance
            } else if (nearest.second > distance) {
                nearest = barrier to distance
            }
        }
        return nearest?.first
    }

    private fun getNearestRelay(
        target: Point,
        relays: List<Relay>,
        maxDistance: Double,
    ): Relay? {
        var nearest: Pair<Relay, Double>? = null
        for (relay in relays) {
            val distance = distanceOf(relay.point, target)
            if (distance.gt(other = maxDistance, points = 12)) continue
            if (nearest == null) {
                nearest = relay to distance
            } else if (nearest.second > distance) {
                nearest = relay to distance
            }
        }
        return nearest?.first
    }

    private fun getNearestItem(
        target: Point,
        items: List<Item>,
        maxDistance: Double,
    ): Item? {
        var nearest: Pair<Item, Double>? = null
        for (item in items) {
            if (item.ownerID != null) continue
            val distance = distanceOf(item.point, target)
            if (distance.gt(other = maxDistance, points = 12)) continue
            if (nearest == null || nearest.second > distance) {
                nearest = item to distance
            }
        }
        return nearest?.first
    }

    private fun onRenderInteraction(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        val point = getNearestBarrier(
            target = env.player.moving.point,
            barriers = env.barriers,
            minDistance = 1.0,
            maxDistance = 1.75,
        )?.vector?.center() ?: getNearestRelay(
            target = env.player.moving.point,
            relays = env.relays,
            maxDistance = 1.75,
        )?.point ?: getNearestItem(
            target = env.player.moving.point,
            items = env.items,
            maxDistance = 1.75,
        )?.point ?: return
        val isPressed = engine.input.keyboard.isPressed(KeyboardButton.F)
        canvas.polygons.drawRectangle(
            borderColor = Color.GREEN,
            fillColor = Color.GREEN.copy(alpha = if (isPressed) 0.5f else 0f),
            pointTopLeft = point,
            size = sizeOf(1.0, 1.0),
            lineWidth = 0.1,
            offset = offset + offsetOf(dX = 1.0, dY = -1.5),
            measure = measure,
        )
        val info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure)
        canvas.texts.draw(
            color = Color.GREEN,
            info = info,
            pointTopLeft = point,
            offset = offset + offsetOf(dX = 1.25, dY = -1.5),
            measure = measure,
            text = "F",
        )
    }

    private fun onRenderItems(
        canvas: Canvas,
        items: List<Item>,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        val size = sizeOf(1.0, 0.75)
        val info = FontInfoUtil.getFontInfo(height = 0.75, measure = measure)
        for (index in items.indices) {
            val item = items[index]
            if (item.ownerID != null) continue
            canvas.polygons.drawRectangle(
                color = Color.GREEN,
                pointTopLeft = item.point,
                size = size,
                offset = offset + size.center() * -1.0,
                measure = measure,
            )
            val text = "i#${index % 10}"
            val textWidth = engine.fontAgent.getTextWidth(info, text)
            canvas.texts.draw(
                color = Color.BLACK,
                info = info,
                pointTopLeft = item.point,
                offset = offset + offsetOf(dX = measure.units(textWidth) / 2, dY = measure.units(info.height.toDouble()) / 2) * -1.0,
                measure = measure,
                text = text,
            )
        }
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
        onRenderPlayer(
            canvas = canvas,
            offset = if (env.isCameraFree()) {
                offset
            } else {
                centerPoint - env.player.moving.point
            },
            measure = measure,
        )
        onRenderWalls(
            canvas = canvas,
            walls = env.walls,
            offset = offset,
            measure = measure,
        )
        onRenderBarriers(
            canvas = canvas,
            barriers = env.barriers,
            offset = offset,
            measure = measure,
        )
        onRenderRelays(
            canvas = canvas,
            relays = env.relays,
            offset = offset,
            measure = measure,
        )
        onRenderItems(
            canvas = canvas,
            items = env.items,
            offset = offset,
            measure = measure,
        )
        //
        onRenderInteraction(
            canvas = canvas,
            offset = offset,
            measure = measure,
        )
        // todo
        //
        if (env.isCameraFree()) {
            onRenderCamera(
                canvas = canvas,
                offset = centerOffset,
                measure = measure,
            )
        }
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
                    id = UUID(101, 1),
                    tags = emptySet(), // todo
                    point = MutablePoint(0.0, 0.0),
                    ownerID = null,
                ),
            )
            return Environment(
                walls = walls,
                player = player,
                camera = camera,
                isCameraFree = false,
                conditions = conditions,
                barriers = barriers,
                relays = relays,
                items = items,
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
