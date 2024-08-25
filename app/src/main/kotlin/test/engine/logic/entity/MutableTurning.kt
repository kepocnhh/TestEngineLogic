package test.engine.logic.entity

import sp.kx.math.measure.MutableDeviation

internal interface MutableTurning : Turning {
    override val direction: MutableDeviation<Double>
}
