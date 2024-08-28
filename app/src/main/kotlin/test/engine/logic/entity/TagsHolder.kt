package test.engine.logic.entity

import java.util.UUID

internal interface TagsHolder {
    val enabled: Boolean
    val tags: Set<UUID>
}
