package test.engine.logic.entity

import java.util.UUID

internal class Player(
    val id: UUID,
    val moving: MutableMoving,
    val turning: MutableTurning,
)
