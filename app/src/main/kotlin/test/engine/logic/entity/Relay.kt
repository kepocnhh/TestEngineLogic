package test.engine.logic.entity

import sp.kx.math.Point
import java.util.UUID

internal class Relay(
    val point: Point,
    var enabled: Boolean,
    val conditions: List<UUID>,
)
