package test.engine.logic

import sp.kx.math.Point
import sp.kx.math.distanceOf
import sp.kx.math.getShortestDistance
import sp.kx.math.gt
import sp.kx.math.lt
import test.engine.logic.entity.Barrier
import test.engine.logic.entity.Condition
import test.engine.logic.entity.Crate
import test.engine.logic.entity.Item
import test.engine.logic.entity.Relay
import test.engine.logic.entity.TagsHolder

internal object Entities {
    fun getNearestRelay(
        target: Point,
        relays: List<Relay>,
        maxDistance: Double,
    ): Relay? {
        var nearest: Pair<Relay, Double>? = null
        for (relay in relays) {
            val distance = distanceOf(relay.point, target)
            if (distance.gt(other = maxDistance, points = 12)) continue
            if (nearest == null) {
                nearest = relay to distance
            } else if (nearest.second > distance) {
                nearest = relay to distance
            }
        }
        return nearest?.first
    }

    fun getNearestItem(
        target: Point,
        items: List<Item>,
        maxDistance: Double,
    ): Item? {
        var nearest: Pair<Item, Double>? = null
        for (item in items) {
            if (item.owner != null) continue
            val distance = distanceOf(item.point, target)
            if (distance.gt(other = maxDistance, points = 12)) continue
            if (nearest == null || nearest.second > distance) {
                nearest = item to distance
            }
        }
        return nearest?.first
    }

    fun getNearestCrate(
        target: Point,
        crates: List<Crate>,
        maxDistance: Double,
    ): Crate? {
        var nearest: Pair<Crate, Double>? = null
        for (crate in crates) {
            val distance = distanceOf(crate.point, target)
            if (distance.gt(other = maxDistance, points = 12)) continue
            if (nearest == null || nearest.second > distance) {
                nearest = crate to distance
            }
        }
        return nearest?.first
    }

    fun getNearestBarrier(
        target: Point,
        barriers: List<Barrier>,
        minDistance: Double,
        maxDistance: Double,
    ): Barrier? {
        var nearest: Pair<Barrier, Double>? = null
        for (barrier in barriers) {
            if (barrier.conditions.isNotEmpty()) continue // todo
            val distance = barrier.vector.getShortestDistance(target)
            if (distance.lt(other = minDistance, points = 12)) continue
            if (distance.gt(other = maxDistance, points = 12)) continue
            if (nearest == null) {
                nearest = barrier to distance
            } else if (nearest.second > distance) {
                nearest = barrier to distance
            }
        }
        return nearest?.first
    }

    private fun isPassed(condition: Condition, holders: List<TagsHolder>): Boolean {
        if (condition.tags == null) return true
        return condition.tags.any { set ->
            set.all { tag ->
                holders.any { holder ->
                    holder.tags.contains(tag) && holder.enabled
                }
            }
        }
    }

    private fun deepPassed(condition: Condition, holders: List<TagsHolder>, conditions: List<Condition>): Boolean {
        if (condition.depends == null) return true
        return condition.depends.any { set ->
            set.all { id ->
                isPassed(
                    condition = conditions.firstOrNull { it.id == id } ?: TODO(),
                    holders = holders,
                    conditions = conditions,
                )
            }
        }
    }

    fun isPassed(condition: Condition, holders: List<TagsHolder>, conditions: List<Condition>): Boolean {
        return isPassed(condition = condition, holders = holders) &&
            deepPassed(condition = condition, holders = holders, conditions = conditions)
    }
}
