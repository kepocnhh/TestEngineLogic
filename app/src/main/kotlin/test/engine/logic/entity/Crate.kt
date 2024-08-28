package test.engine.logic.entity

import sp.kx.math.Point
import java.util.UUID

internal class Crate(
    val id: UUID,
    val point: Point,
    val lock: Lock,
)
