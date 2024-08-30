package test.engine.logic.entity

import sp.kx.math.Vector

internal class Barrier(
    val vector: Vector,
    var opened: Boolean,
    val lock: Lock,
)
