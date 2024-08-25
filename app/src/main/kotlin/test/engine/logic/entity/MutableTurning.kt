package test.engine.logic.entity

import sp.kx.math.dby
import sp.kx.math.eq
import sp.kx.math.ifNaN
import sp.kx.math.measure.MutableDeviation
import sp.kx.math.measure.diff
import sp.kx.math.radians
import sp.kx.math.whc
import kotlin.math.absoluteValue
import kotlin.time.Duration

internal interface MutableTurning : Turning {
    override val direction: MutableDeviation<Double>

    fun turn(radians: Double, timeDiff: Duration) {
        direction.expected = radians
        val dirDiff = direction.diff()
        if (dirDiff.absoluteValue.eq(0.0, points = 4)) return
        val alpha = directionSpeed.length(timeDiff)
        if (alpha > dirDiff.absoluteValue) {
            direction.commit()
        } else {
            val k = dirDiff.absoluteValue.whc().ifNaN(1.0)
            val m = dirDiff.dby()
            val actual = direction.actual + alpha * m * k
            direction.actual = actual.radians()
        }
    }
}
