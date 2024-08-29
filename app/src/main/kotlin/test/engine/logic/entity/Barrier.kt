package test.engine.logic.entity

import sp.kx.math.Vector
import java.util.UUID

internal class Barrier(
    val vector: Vector,
    var opened: Boolean,
    val conditions: List<Set<UUID>>?,
)
