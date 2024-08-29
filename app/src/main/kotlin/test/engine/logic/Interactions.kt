package test.engine.logic

import sp.kx.lwjgl.entity.input.KeyboardButton
import test.engine.logic.entity.Barrier
import test.engine.logic.entity.Crate
import test.engine.logic.entity.Item
import test.engine.logic.entity.Lock
import test.engine.logic.entity.Player
import test.engine.logic.entity.Relay
import java.util.UUID

internal class Interactions(private val env: Environment) {
    private fun onInteractionBarrier(barrier: Barrier) {
        barrier.opened = !barrier.opened // todo
    }

    private fun isOpened(lock: Lock, items: Iterable<Item>): Boolean {
        if (lock.required == null) return true
        return lock.required.any { tags ->
            tags.all { tag ->
                items.any { item ->
                    item.tags.contains(tag)
                }
            }
        }
    }

    private fun isOpened(lock: Lock?): Boolean {
        if (lock == null) return true
        if (lock.opened == true) return true
        val opened = isOpened(
            lock = lock,
            items = env.items.filter { it.owner == env.player.id },
        )
        if (lock.opened == false) {
            lock.opened = opened
        }
        return opened
    }

    private fun onInteractionRelay(relay: Relay) {
        if (!isOpened(lock = relay.lock)) return
        relay.enabled = !relay.enabled
        for (barrier in env.barriers) {
            if (barrier.conditions == null) continue
            barrier.opened = Entities.deepPassed(
                depends = barrier.conditions,
                holders = env.relays,
                conditions = env.conditions,
            )
        }
    }

    private fun onInteractionItem(item: Item) {
        item.owner = env.player.id
    }

    private fun onInteractionCrate(crate: Crate) {
        if (!isOpened(lock = crate.lock)) return
        env.state = Environment.State.Swap(
            index = 0,
            side = true,
            src = env.player.id,
            dst = crate.id,
        )
    }

    private fun onInteraction() {
        val barrier = Entities.getNearestBarrier(
            target = env.player.moving.point,
            barriers = env.barriers,
            minDistance = 1.0,
            maxDistance = 1.75,
        )
        if (barrier != null) {
            onInteractionBarrier(barrier = barrier)
            return
        }
        val relay = Entities.getNearestRelay(
            target = env.player.moving.point,
            relays = env.relays,
            maxDistance = 1.75,
        )
        if (relay != null) {
            onInteractionRelay(relay = relay)
            return
        }
        val item = Entities.getNearestItem(
            target = env.player.moving.point,
            items = env.items,
            maxDistance = 1.75,
        )
        if (item != null) {
            onInteractionItem(item = item)
            return
        }
        val crate = Entities.getNearestCrate(
            target = env.player.moving.point,
            crates = env.crates,
            maxDistance = 1.75,
        )
        if (crate != null) {
            onInteractionCrate(crate = crate)
            return
        }
    }

    private fun onPressWalking(button: KeyboardButton) {
        when (button) {
            KeyboardButton.C -> env.switchCamera()
            KeyboardButton.F -> onInteraction()
            KeyboardButton.TAB -> {
                env.state = Environment.State.Inventory(index = 0)
            }
            else -> {/*noop*/}
        }
    }

    private fun onPressInventory(state: Environment.State.Inventory, button: KeyboardButton) {
        when (button) {
            KeyboardButton.TAB, KeyboardButton.ESCAPE -> {
                env.state = Environment.State.Walking
            }
            else -> {/*noop*/}
        }
        val items = env.items.filter { it.owner == env.player.id }
        if (items.isEmpty()) return
        when (button) {
            KeyboardButton.W -> {
                state.index = (items.size + state.index - 1) % items.size
            }
            KeyboardButton.S -> {
                state.index = (state.index + 1) % items.size
            }
            KeyboardButton.X -> {
                val item = items[state.index]
                item.point.set(env.player.moving.point)
                item.owner = null
                if (state.index == items.lastIndex) {
                    state.index = (items.size + state.index - 1) % items.size
                }
            }
            else -> {/*noop*/}
        }
    }

    private fun onPressSwap(state: Environment.State.Swap, button: KeyboardButton) {
        when (button) {
            KeyboardButton.ESCAPE -> {
                env.state = Environment.State.Walking
            }
            else -> {/*noop*/}
        }
        val owner = if (state.side) state.src else state.dst
        val items = env.items.filter { it.owner == owner }
        if (items.isNotEmpty()) when (button) {
            KeyboardButton.W -> {
                state.index = (items.size + state.index - 1) % items.size
            }
            KeyboardButton.S -> {
                state.index = (state.index + 1) % items.size
            }
            KeyboardButton.F -> {
                val item = items[state.index]
                item.point.set(env.player.moving.point)
                item.owner = if (state.side) state.dst else state.src
                if (state.index == items.lastIndex) {
                    state.index = (items.size + state.index - 1) % items.size
                }
            }
            else -> {/*noop*/}
        }
        when (button) {
            KeyboardButton.A, KeyboardButton.D -> {
                state.index = 0
                state.side = !state.side
            }
            else -> {/*noop*/}
        }
    }

    fun onPress(button: KeyboardButton) {
        when (val state = env.state) {
            Environment.State.Walking -> onPressWalking(button = button)
            is Environment.State.Inventory -> onPressInventory(state = state, button = button)
            is Environment.State.Swap -> onPressSwap(state = state, button = button)
        }
    }
}
