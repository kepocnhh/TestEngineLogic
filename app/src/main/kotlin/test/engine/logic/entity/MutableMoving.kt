package test.engine.logic.entity

import sp.kx.math.MutablePoint
import sp.kx.math.measure.MutableSpeed

internal interface MutableMoving : Moving {
    override val point: MutablePoint
    override val speed: MutableSpeed
}
