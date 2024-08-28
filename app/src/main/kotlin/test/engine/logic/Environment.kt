package test.engine.logic

import sp.kx.math.Vector
import test.engine.logic.entity.Barrier
import test.engine.logic.entity.Condition
import test.engine.logic.entity.Crate
import test.engine.logic.entity.Item
import test.engine.logic.entity.MutableMoving
import test.engine.logic.entity.Player
import test.engine.logic.entity.Relay
import java.util.UUID

internal class Environment(
    var state: State,
    val walls: List<Vector>,
    val player: Player,
    val camera: MutableMoving,
    private var isCameraFree: Boolean,
    val conditions: List<Condition>,
    val barriers: List<Barrier>,
    val relays: List<Relay>,
    val items: List<Item>,
    val crates: List<Crate>,
) {
    sealed interface State {
        data object Walking : State
        class Inventory(var index: Int) : State
        class Swap(
            var index: Int,
            var side: Boolean,
            val src: UUID,
            val dst: UUID,
        ) : State
    }

    fun isCameraFree(): Boolean {
        return isCameraFree
    }

    fun switchCamera() {
        camera.point.set(player.moving.point)
        isCameraFree = !isCameraFree
    }
}
