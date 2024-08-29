package test.engine.logic.entity

import java.util.UUID

internal class Condition(
    val id: UUID,
    val depends: List<Set<UUID>>?,
    val tags: List<Set<UUID>>?,
)
