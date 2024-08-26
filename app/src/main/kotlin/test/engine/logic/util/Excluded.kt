package test.engine.logic.util

import org.lwjgl.opengl.GL11
import sp.kx.lwjgl.entity.Color
import sp.kx.lwjgl.opengl.GLUtil
import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.Size
import sp.kx.math.Vector
import sp.kx.math.angle
import sp.kx.math.getShortestDistance
import sp.kx.math.lt
import sp.kx.math.measure.Measure
import sp.kx.math.offsetOf
import sp.kx.math.plus
import sp.kx.math.pointOf
import sp.kx.math.sizeOf

@Deprecated(message = "polygon")
internal fun List<Point>.toVectors(): List<Vector> {
    if (size < 2) TODO()
    // 0  1  2  3  4
    // *--*--*--*--*
    val list = ArrayList<Vector>(size - 1)
    for (index in 1 until size) {
        list += get(index - 1) + get(index)
    }
    return list
}

@Deprecated(message = "sp.kx.math.plus")
internal operator fun Size.plus(
    measure: Measure<Double, Double>,
): Size {
    return sizeOf(
        width = measure.transform(width),
        height = measure.transform(height),
    )
}

@Deprecated(message = "sp.kx.math.minus")
internal operator fun Size.minus(
    measure: Measure<Double, Double>,
): Size {
    return sizeOf(
        width = measure.units(width),
        height = measure.units(height),
    )
}

@Deprecated(message = "sp.kx.math.div")
internal operator fun Size.div(value: Double): Size {
    return sizeOf(
        width = width / value,
        height = height / value,
    )
}

@Deprecated(message = "sp.kx.math.minus")
internal operator fun Point.minus(measure: Measure<Double, Double>): Point {
    return pointOf(
        x = measure.units(x),
        y = measure.units(y),
    )
}

@Deprecated(message = "sp.kx.math.minus")
internal operator fun Offset.minus(measure: Measure<Double, Double>): Offset {
    return offsetOf(
        dX = measure.units(dX),
        dY = measure.units(dY),
    )
}

private fun vertexOf(
    vector: Vector,
    lineWidth: Double,
    offset: Offset,
    measure: Measure<Double, Double>,
) {
    val angle = vector.angle()
    GLUtil.vertexOfMoved(vector.start, length = lineWidth / 2, angle = angle - kotlin.math.PI / 2, offset = offset, measure = measure)
    GLUtil.vertexOfMoved(vector.start, length = lineWidth / 2, angle = angle + kotlin.math.PI / 2, offset = offset, measure = measure)
    GLUtil.vertexOfMoved(vector.finish, length = lineWidth / 2, angle = angle - kotlin.math.PI / 2, offset = offset, measure = measure)
    GLUtil.vertexOfMoved(vector.finish, length = lineWidth / 2, angle = angle + kotlin.math.PI / 2, offset = offset, measure = measure)
}

@Deprecated(message = "sp.kx.math.draw")
internal fun drawVectors(
    color: Color,
    vectors: List<Vector>,
    offset: Offset,
    measure: Measure<Double, Double>,
    lineWidth: Double,
) {
    GL11.glLineWidth(1f)
    GLUtil.colorOf(color)
    GLUtil.transaction(GL11.GL_TRIANGLE_STRIP) {
        val first = vectors.first()
        vertexOf(vector = first, lineWidth = lineWidth, offset = offset, measure = measure)
        for (i in 1 until vectors.size) {
            vertexOf(vector = vectors[i], lineWidth = lineWidth, offset = offset, measure = measure)
        }
//        GLUtil.vertexOfMoved(first.start, length = lineWidth / 2, angle = first.angle() - kotlin.math.PI / 2, offset = offset, measure = measure)
    }
}

@Deprecated(message = "sp.kx.math.closerThan")
internal fun Vector.closerThan(point: Point, minDistance: Double): Boolean {
    return getShortestDistance(point).lt(other = minDistance, points = 12)
}
