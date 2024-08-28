package test.engine.logic.entity

import sp.kx.math.Point
import java.util.UUID

internal class Relay(
    val point: Point,
    override var enabled: Boolean,
    override val tags: Set<UUID>,
) : TagsHolder
