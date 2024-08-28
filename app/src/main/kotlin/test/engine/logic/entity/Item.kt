package test.engine.logic.entity

import sp.kx.math.MutablePoint
import java.util.UUID

internal class Item(
    val id: UUID,
    val tags: Set<UUID>,
    val point: MutablePoint,
    var ownerID: UUID?,
)
