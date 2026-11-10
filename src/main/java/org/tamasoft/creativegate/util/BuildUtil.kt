package org.tamasoft.creativegate.util

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.tamasoft.creativegate.State

/**
 * Asks protection plugins (Lands, WorldGuard, ...) whether the player may build at a block by firing a
 * BlockPlaceEvent that places nothing. Never use a BlockBreakEvent for this: our own MONITOR listener would
 * destroy the gate whose frame block the event names.
 */
object BuildUtil {

    fun canBuild(player: Player, block: Block, against: Block, hand: EquipmentSlot = EquipmentSlot.HAND): Boolean {
        val item = player.inventory.getItem(hand).takeIf { !it.type.isAir } ?: ItemStack(Material.STONE)
        val event = BlockPlaceEvent(block, block.state, against, item, player, true, hand)

        // Our own listener cancels placing into portal blocks of registered gates; that is not a protection answer.
        val previous = State.isProbing
        State.isProbing = true
        try {
            Bukkit.getPluginManager().callEvent(event)
        } finally {
            State.isProbing = previous
        }
        return !event.isCancelled && event.canBuild()
    }

    /** True when the player may build at every one of the blocks. */
    fun canBuildAll(player: Player, blocks: Collection<Block>, against: Block, hand: EquipmentSlot): Boolean {
        return blocks.all { canBuild(player, it, against, hand) }
    }
}
