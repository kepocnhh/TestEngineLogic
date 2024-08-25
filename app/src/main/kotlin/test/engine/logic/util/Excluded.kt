package test.engine.logic.util

import sp.kx.math.Point
import sp.kx.math.Size
import sp.kx.math.Vector
import sp.kx.math.measure.Measure
import sp.kx.math.plus
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
