package test.engine.logic.entity

import java.util.UUID

internal class Lock(
    var opened: Boolean?,
    val required: List<Set<UUID>>?,
)
