package test.engine.logic.entity

import sp.kx.math.measure.Deviation
import sp.kx.math.measure.Speed

internal interface Turning {
    val direction: Deviation<Double>
    val directionSpeed: Speed
}
