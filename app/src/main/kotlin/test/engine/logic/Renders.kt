package test.engine.logic

import sp.kx.lwjgl.engine.Engine
import sp.kx.lwjgl.entity.Canvas
import sp.kx.lwjgl.entity.Color
import sp.kx.lwjgl.entity.font.FontInfo
import sp.kx.lwjgl.entity.input.KeyboardButton
import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.Size
import sp.kx.math.Vector
import sp.kx.math.angle
import sp.kx.math.minus
import sp.kx.math.center
import sp.kx.math.centerPoint
import sp.kx.math.copy
import sp.kx.math.measure.Measure
import sp.kx.math.measure.frequency
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
import test.engine.logic.util.FontInfoUtil
import test.engine.logic.util.diagonal
import test.engine.logic.util.diagonalAngle
import test.engine.logic.util.drawRectangle
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
            if (item.owner != null) continue
            canvas.polygons.drawRectangle(
                color = Color.GREEN,
                pointTopLeft = item.point,
                size = size,
                offset = offset + size.center() * -1.0,
                measure = measure,
            )
            val text = "i${index % 10}"
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

    private fun onRenderCrates(
        canvas: Canvas,
        crates: List<Crate>,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        val size = sizeOf(1.0, 1.0)
        val info = FontInfoUtil.getFontInfo(height = 0.75, measure = measure)
        for (index in crates.indices) {
            val crate = crates[index]
            when (val lock = crate.lock) {
                null -> Unit
                else -> when (lock.opened) {
                    null -> {
                        canvas.vectors.draw(
                            color = Color.YELLOW.copy(alpha = 0.5f),
                            vector = (crate.point + size.center()) + (crate.point + size.center() * -1.0),
                            offset = offset,
                            measure = measure,
                            lineWidth = 0.1,
                        )
                    }
                    false -> {
                        canvas.vectors.draw(
                            color = Color.RED,
                            vector = (crate.point + size.center()) + (crate.point + size.center() * -1.0),
                            offset = offset,
                            measure = measure,
                            lineWidth = 0.1,
                        )
                    }
                    else -> Unit
                }
            }
            canvas.polygons.drawRectangle(
                color = Color.YELLOW,
                pointTopLeft = crate.point,
                size = size,
                offset = offset + size.center() * -1.0,
                measure = measure,
                lineWidth = 0.1,
            )
            val text = "c${index % 10}"
            val textWidth = engine.fontAgent.getTextWidth(info, text)
            canvas.texts.draw(
                color = Color.YELLOW,
                info = info,
                pointTopLeft = crate.point,
                offset = offset + offsetOf(dX = measure.units(textWidth) / 2, dY = measure.units(info.height.toDouble()) / 2) * -1.0,
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
        size: Size,
        items: List<Item>,
        info: FontInfo,
        selected: Int?,
        title: String,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        canvas.polygons.drawRectangle(
            borderColor = Color.GREEN,
            fillColor = Color.BLACK.copy(alpha = 0.75f),
            pointTopLeft = Point.Center + offset,
            size = size,
            lineWidth = 0.1,
            measure = measure,
        )
        if (title.isNotBlank()) {
            canvas.texts.draw(
                info = info,
                pointTopLeft = Point.Center + offset + offsetOf(1, -1),
                measure = measure,
                color = if (selected == null) Color.GREEN else Color.YELLOW,
                text = title,
            )
        }
        if (items.isEmpty()) {
            canvas.texts.draw(
                info = info,
                pointTopLeft = Point.Center + offset + offsetOf(0.75, 0.5),
                measure = measure,
                color = Color.GREEN.copy(alpha = 0.5f),
                text = "no items",
            )
            return
        }
        for (index in items.indices) {
            val item = items[index]
            val isSelected = selected == index
            val color = if (isSelected) Color.YELLOW else Color.GREEN
            val text = "#${env.items.indexOf(item)} ${item.id.toString().substring(0, 4)}"
            val prefix = if (isSelected) "> " else "  "
            canvas.texts.draw(
                info = info,
                pointTopLeft = Point.Center + offset + offsetOf(0.75, 0.5) + Offset.Empty.copy(dY = index * measure.units(info.height.toDouble())),
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
            info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure),
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
        onRenderItems(
            canvas = canvas,
            size = size,
            items = env.items.filter { it.owner == state.src },
            info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure),
            selected = state.index.takeIf { state.side },
            title = "",
            offset = offsetOf(2, 2),
            measure = measure,
        )
        onRenderItems(
            canvas = canvas,
            size = size,
            items = env.items.filter { it.owner == state.dst },
            info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure),
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

    private fun onRenderDebug(
        canvas: Canvas,
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

    fun onRender(canvas: Canvas, measure: Measure<Double, Double>) {
        val fps = engine.property.time.frequency()
//        canvas.texts.draw(
//            info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure),
//            pointTopLeft = Point.Center,
//            color = Color.GREEN,
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
