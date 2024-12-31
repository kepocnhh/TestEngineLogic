package test.engine.logic

import sp.kx.lwjgl.engine.Engine
import sp.kx.lwjgl.entity.Canvas
import sp.kx.lwjgl.entity.Color
import sp.kx.lwjgl.entity.copy
import sp.kx.lwjgl.entity.input.KeyboardButton
import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.Size
import sp.kx.math.Vector
import sp.kx.math.angle
import sp.kx.math.center
import sp.kx.math.centerPoint
import sp.kx.math.copy
import sp.kx.math.measure.Measure
import sp.kx.math.measure.frequency
import sp.kx.math.minus
import sp.kx.math.moved
import sp.kx.math.offsetOf
import sp.kx.math.plus
import sp.kx.math.pointOf
import sp.kx.math.sizeOf
import sp.kx.math.times
import sp.kx.math.vectorOf
import test.engine.logic.entity.Barrier
import test.engine.logic.entity.Crate
import test.engine.logic.entity.Item
import test.engine.logic.entity.Relay
import test.engine.logic.util.diagonal
import test.engine.logic.util.diagonalAngle
import test.engine.logic.util.minus
import test.engine.logic.util.plus

internal class Renders(
    private val engine: Engine,
    private val env: Environment,
) {
    private fun onRenderPlayer(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        canvas.polygons.drawCircle(
            borderColor = Color.Blue,
            fillColor = Color.Blue.copy(alpha = 0.5f),
            pointCenter = env.player.moving.point,
            radius = 1.0,
            edgeCount = 16,
            lineWidth = 0.1,
            offset = offset,
            measure = measure,
        )
        canvas.vectors.draw(
            color = Color.Yellow,
            vector = vectorOf(env.player.moving.point, length = 1.0, angle = env.player.turning.direction.expected),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
        canvas.vectors.draw(
            color = Color.White,
            vector = vectorOf(env.player.moving.point, length = 1.0, angle = env.player.turning.direction.actual),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
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
            color = Color.Gray,
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
            val angle = barrier.vector.angle()
            val startPoint = barrier.vector.start.moved(length = size.diagonal() / 2, angle = angle - kotlin.math.PI / 2 - size.diagonalAngle())
            val finishPoint = barrier.vector.finish.moved(length = size.diagonal() / 2, angle = angle - kotlin.math.PI / 2 - size.diagonalAngle())
            if (!barrier.opened) {
                canvas.vectors.draw(
                    color = Color.Red,
                    vector = barrier.vector,
                    offset = offset,
                    measure = measure,
                    lineWidth = 0.2,
                )
                if (barrier.lock.opened == false) {
                    canvas.polygons.drawRectangle(
                        color = Color.Yellow,
                        pointTopLeft = startPoint.moved(length = 0.5, angle = angle),
                        size = size,
                        pointOfRotation = startPoint.moved(length = 0.5, angle = angle),
                        offset = offset,
                        measure = measure,
                        direction = angle,
                    )
                } else if (barrier.lock.opened == null && barrier.lock.required != null) {
                    canvas.polygons.drawRectangle(
                        color = Color.Yellow,
                        pointTopLeft = startPoint.moved(length = 0.5, angle = angle),
                        size = size,
                        pointOfRotation = startPoint.moved(length = 0.5, angle = angle),
                        offset = offset,
                        measure = measure,
                        direction = angle,
                        lineWidth = 0.05,
                    )
                }
                if (barrier.lock.conditions != null) {
                    val passed = Entities.deepPassed(
                        depends = barrier.lock.conditions,
                        holders = env.relays,
                        conditions = env.conditions,
                    )
                    if (!passed) {
                        canvas.polygons.drawRectangle(
                            color = Color.Red,
                            pointTopLeft = startPoint.moved(length = 1.0, angle = angle),
                            size = size,
                            pointOfRotation = startPoint.moved(length = 1.0, angle = angle),
                            offset = offset,
                            measure = measure,
                            direction = angle,
                        )
                    }
                }
                if (barrier.conditions != null) {
                    canvas.polygons.drawRectangle(
                        color = Color.Red,
                        pointTopLeft = finishPoint.moved(length = 1.0, angle = angle + kotlin.math.PI),
                        size = size,
                        pointOfRotation = finishPoint.moved(length = 1.0, angle = angle + kotlin.math.PI),
                        offset = offset,
                        measure = measure,
                        direction = angle,
                    )
                }
            }
            canvas.polygons.drawRectangle(
                color = Color.Yellow,
                pointTopLeft = startPoint,
                size = size,
                pointOfRotation = startPoint,
                offset = offset,
                measure = measure,
                direction = angle,
            )
            canvas.polygons.drawRectangle(
                color = Color.Yellow,
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
//        val info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure)
        val dHeight = size.height * 0.75
        val d = size.height - dHeight
        val dSize = sizeOf(width = dHeight, height = dHeight)
        for (relay in relays) {
            val color = if (relay.enabled) Color.Green else Color.Red
            canvas.polygons.drawRectangle(
                color = color,
//                borderColor = color,
//                fillColor = color.copy(alpha = 0.5f),
                pointTopLeft = relay.point,
                size = size,
                offset = offset + size.center() * -1.0,
                measure = measure,
//                lineWidth = d / 2,
            )
            when (val lock = relay.lock) {
                null -> Unit
                else -> when (lock.opened) {
                    true -> Unit
                    else -> {
                        val start = relay.point + offsetOf(dX = size.width / 2, dY = - size.height / 2)
                        val finish = relay.point + offsetOf(dX = size.width / 2, dY = size.height / 2)
                        val vector = start + finish
//                        canvas.vectors.draw(
//                            color = Color.Yellow,
//                            vector = vector,
//                            lineWidth = 0.1,
//                            offset = offset,
//                            measure = measure,
//                        )
                    }
                }
            }
//            val text = if (relay.enabled) "on" else "off"
//            val textWidth = engine.fontAgent.getTextWidth(info, text)
//            canvas.texts.draw(
//                color = Color.Black,
//                info = info,
//                pointTopLeft = relay.point,
//                offset = offset + offsetOf(dX = measure.units(textWidth) / 2, dY = size.height / 2) * -1.0,
//                measure = measure,
//                text = text,
//            )
            val dX = if (relay.enabled) {
                size.width / 2 - d / 2 - dSize.width
            } else {
                d / 2 - size.width / 2
            }
//            canvas.polygons.drawRectangle(
//                color = Color.Black.copy(alpha = 0.5f),
//                pointTopLeft = relay.point + offsetOf(dX = d / 2 - size.width / 2, dY = - dSize.height / 2),
//                size = sizeOf(width = size.width - d, height = dHeight),
//                offset = offset,
//                measure = measure,
//            )
            canvas.polygons.drawRectangle(
                color = Color.Black,
                pointTopLeft = relay.point + offsetOf(dX = dX, dY = - dSize.height / 2),
                size = dSize,
                offset = offset,
                measure = measure,
            )
        }
    }

    private fun onRenderItems(
        canvas: Canvas,
        items: List<Item>,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        val size = sizeOf(1.0, 0.75)
        val fontHeight = 0.75
        for (index in items.indices) {
            val item = items[index]
            if (item.owner != null) continue
            canvas.polygons.drawRectangle(
                color = Color.Green,
                pointTopLeft = item.point,
                size = size,
                offset = offset + size.center() * -1.0,
                measure = measure,
            )
            val text = "i${index % 10}"
            val textWidth = canvas.texts.getTextWidth(fontHeight = fontHeight, text = text, measure = measure)
            canvas.texts.draw(
                color = Color.Black,
                fontHeight = fontHeight,
                pointTopLeft = item.point,
                offset = offset + offsetOf(dX = textWidth / 2, dY = fontHeight / 2) * -1.0,
                measure = measure,
                text = text,
            )
        }
    }

    private fun onRenderCrates(
        canvas: Canvas,
        crates: List<Crate>,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        val size = sizeOf(1.0, 1.0)
        val fontHeight = 0.75
        for (index in crates.indices) {
            val crate = crates[index]
            when (crate.lock.opened) {
                null -> {
                    canvas.vectors.draw(
                        color = Color.Yellow.copy(alpha = 0.5f),
                        vector = (crate.point + size.center()) + (crate.point + size.center() * -1.0),
                        offset = offset,
                        measure = measure,
                        lineWidth = 0.1,
                    )
                }
                false -> {
                    canvas.vectors.draw(
                        color = Color.Red,
                        vector = (crate.point + size.center()) + (crate.point + size.center() * -1.0),
                        offset = offset,
                        measure = measure,
                        lineWidth = 0.1,
                    )
                }
                else -> Unit
            }
            canvas.polygons.drawRectangle(
                color = Color.Yellow,
                pointTopLeft = crate.point,
                size = size,
                offset = offset + size.center() * -1.0,
                measure = measure,
                lineWidth = 0.1,
            )
            val text = "c${index % 10}"
            val textWidth = canvas.texts.getTextWidth(fontHeight = fontHeight, text = text, measure = measure)
            canvas.texts.draw(
                color = Color.Yellow,
                fontHeight = fontHeight,
                pointTopLeft = crate.point,
                offset = offset + offsetOf(dX = textWidth / 2, dY = fontHeight / 2) * -1.0,
                measure = measure,
                text = text,
            )
        }
    }

    private fun onRenderInteraction(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        val point = Entities.getNearestBarrier(
            target = env.player.moving.point,
            barriers = env.barriers,
            minDistance = 1.0,
            maxDistance = 1.75,
        )?.vector?.center() ?: Entities.getNearestRelay(
            target = env.player.moving.point,
            relays = env.relays,
            maxDistance = 1.75,
        )?.point ?: Entities.getNearestItem(
            target = env.player.moving.point,
            items = env.items,
            maxDistance = 1.75,
        )?.point ?: Entities.getNearestCrate(
            target = env.player.moving.point,
            crates = env.crates,
            maxDistance = 1.75,
        )?.point ?: return
        val isPressed = engine.input.keyboard.isPressed(KeyboardButton.F)
        canvas.polygons.drawRectangle(
            borderColor = Color.Green,
            fillColor = Color.Green.copy(alpha = if (isPressed) 0.5f else 0f),
            pointTopLeft = point,
            size = sizeOf(1.0, 1.0),
            lineWidth = 0.1,
            offset = offset + offsetOf(dX = 1.0, dY = -1.5),
            measure = measure,
        )
        canvas.texts.draw(
            color = Color.Green,
            fontHeight = 1.0,
            pointTopLeft = point,
            offset = offset + offsetOf(dX = 1.25, dY = -1.5),
            measure = measure,
            text = "F",
        )
    }

    private fun onRenderItems(
        canvas: Canvas,
        size: Size,
        items: List<Item>,
        fontHeight: Double,
        selected: Int?,
        title: String,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        canvas.polygons.drawRectangle(
            borderColor = Color.Green,
            fillColor = Color.Black.copy(alpha = 0.75f),
            pointTopLeft = Point.Center + offset,
            size = size,
            lineWidth = 0.1,
            measure = measure,
        )
        if (title.isNotBlank()) {
            canvas.texts.draw(
                fontHeight = fontHeight,
                pointTopLeft = Point.Center + offset + offsetOf(1, -1),
                measure = measure,
                color = if (selected == null) Color.Green else Color.Yellow,
                text = title,
            )
        }
        if (items.isEmpty()) {
            canvas.texts.draw(
                fontHeight = fontHeight,
                pointTopLeft = Point.Center + offset + offsetOf(0.75, 0.5),
                measure = measure,
                color = Color.Green.copy(alpha = 0.5f),
                text = "no items",
            )
            return
        }
        for (index in items.indices) {
            val item = items[index]
            val isSelected = selected == index
            val color = if (isSelected) Color.Yellow else Color.Green
            val text = "#${env.items.indexOf(item)} ${item.id.toString().substring(0, 4)}"
            val prefix = if (isSelected) "> " else "  "
            canvas.texts.draw(
                fontHeight = fontHeight,
                pointTopLeft = Point.Center + offset + offsetOf(0.75, 0.5) + Offset.Empty.copy(dY = index * fontHeight),
                measure = measure,
                color = color,
                text = prefix + text, // todo
            )
        }
    }

    private fun onRenderInventory(
        canvas: Canvas,
        state: Environment.State.Inventory,
        measure: Measure<Double, Double>,
    ) {
        onRenderItems(
            canvas = canvas,
            size = sizeOf(8, 8),
            items = env.items.filter { it.owner == env.player.id },
            fontHeight = 1.0,
            selected = state.index,
            title = "",
            offset = offsetOf(2, 2),
            measure = measure,
        )
    }

    private fun onRenderSwap(
        canvas: Canvas,
        state: Environment.State.Swap,
        measure: Measure<Double, Double>,
    ) {
        val size = sizeOf(8, 8)
        val fontHeight = 1.0
        onRenderItems(
            canvas = canvas,
            size = size,
            items = env.items.filter { it.owner == state.src },
            fontHeight = fontHeight,
            selected = state.index.takeIf { state.side },
            title = "",
            offset = offsetOf(2, 2),
            measure = measure,
        )
        onRenderItems(
            canvas = canvas,
            size = size,
            items = env.items.filter { it.owner == state.dst },
            fontHeight = fontHeight,
            selected = state.index.takeIf { !state.side },
            title = state.dst.toString().substring(0, 4),
            offset = offsetOf(2.0 + size.width + 2.0, 2.0),
            measure = measure,
        )
    }

    private fun onRenderCamera(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        canvas.vectors.draw(
            color = Color.Green,
            vector = vectorOf(-1.0, 0.0, 1.0, 0.0),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
        canvas.vectors.draw(
            color = Color.Green,
            vector = vectorOf(0.0, -1.0, 0.0, 1.0),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
        val fontHeight = 0.75
        canvas.texts.draw(
            color = Color.Green,
            fontHeight = fontHeight,
            pointTopLeft = Point.Center.moved(0.5),
            text = String.format("x: %.2f y: %.2f", env.camera.point.x, env.camera.point.y),
            offset = offset,
            measure = measure,
        )
    }

    private fun onRenderGrid(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
        point: Point,
    ) {
        val pictureSize = engine.property.pictureSize - measure
        canvas.vectors.draw(
            color = Color.White,
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
            color = Color.Green,
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
            color = Color.White,
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
            color = Color.Green,
            vector = vectorOf(
                startX = 1.0,
                startY = 0.0,
                finishX = 1.0,
                finishY = pictureSize.height,
            ),
            lineWidth = 0.1,
            measure = measure,
        )
        val fontHeight = 0.75
        val xHalf = pictureSize.width.toInt() / 2
        val xNumber = kotlin.math.ceil(point.x).toInt()
        val xNumbers = (xNumber - xHalf - 2)..(xNumber + xHalf)
        for (x in xNumbers) {
            val textY = if (x % 2 == 0) 1.0 else 0.25
            canvas.texts.draw(
                color = Color.Green,
                fontHeight = fontHeight,
                pointTopLeft = pointOf(x = x + offset.dX + 0.25, y = textY),
                measure = measure,
                text = String.format("%d", x),
            )
            val lineY = if (x % 2 == 0) 1.5 else 0.5
            canvas.vectors.draw(
                color = Color.Green,
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
                color = Color.Green,
                fontHeight = fontHeight,
                pointTopLeft = pointOf(x = textX, y = y + offset.dY),
                measure = measure,
                text = String.format("%d", y),
            )
            val lineX = if (y % 2 == 0) 0.5 else 1.5
            canvas.vectors.draw(
                color = Color.Green,
                vector = pointOf(x = 1.0, y = y + offset.dY) + pointOf(x = lineX, y = y + offset.dY),
                lineWidth = 0.1,
                measure = measure,
            )
        }
    }

    private fun onRenderDebug(
        canvas: Canvas,
        measure: Measure<Double, Double>,
    ) {
        val fontHeight = 0.75
        val pictureSize = engine.property.pictureSize - measure
        var y = 0
        canvas.texts.draw(
            color = Color.Green,
            fontHeight = fontHeight,
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
            color = Color.Green,
            fontHeight = fontHeight,
            pointTopLeft = pointOf(
                x = 4.0,
                y = 2.0 + y++,
            ),
            text = String.format("Player: {x: %.2f, y: %.2f}", point.x, point.y),
            measure = measure,
        )
    }

    fun onRender(canvas: Canvas, measure: Measure<Double, Double>) {
        val fps = engine.property.time.frequency()
//        canvas.texts.draw(
//            info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure),
//            pointTopLeft = Point.Center,
//            color = Color.Green,
//            text = String.format("%.2f", fps),
//            measure = measure,
//        )
        //
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
        onRenderCrates(
            canvas = canvas,
            crates = env.crates,
            offset = offset,
            measure = measure,
        )
        // todo
        //
        when (val state = env.state) {
            Environment.State.Walking -> {
                onRenderInteraction(
                    canvas = canvas,
                    offset = offset,
                    measure = measure,
                )
            }
            is Environment.State.Inventory -> {
                onRenderInventory(
                    canvas = canvas,
                    state = state,
                    measure = measure,
                )
            }
            is Environment.State.Swap -> {
                onRenderSwap(
                    canvas = canvas,
                    state = state,
                    measure = measure,
                )
            }
        }
        //
        if (env.isCameraFree()) {
            onRenderCamera(
                canvas = canvas,
                offset = centerOffset,
                measure = measure,
            )
        }
        //
        if (engine.input.keyboard.isPressed(KeyboardButton.I)) {
            onRenderGrid(
                canvas = canvas,
                offset = offset,
                measure = measure,
                point = point,
            )
            onRenderDebug(
                canvas = canvas,
                measure = measure,
            )
        }
    }
}
