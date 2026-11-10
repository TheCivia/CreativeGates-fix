package org.tamasoft.creativegate.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tamasoft.creativegate.CreativeGate
import java.lang.reflect.Method
import java.util.UUID
import java.util.logging.Level

/**
 * Gates teleport with cause PLUGIN, which PvPManager does not block unless "Unsafe Teleports" is on, so a gate was
 * an instant escape from a fight. This keeps the time of the last damage per player and asks PvPManager (soft
 * dependency, through reflection so there is no compile-time dependency) for its combat tag.
 */
object CombatUtil {

    private const val MESSAGE_THROTTLE_MILLIS = 2000L

    private val lastDamage: MutableMap<UUID, Long> = HashMap()
    private val lastMessage: MutableMap<UUID, Long> = HashMap()

    fun markDamaged(player: Player) {
        lastDamage[player.uniqueId] = System.currentTimeMillis()
    }

    fun forget(player: Player) {
        lastDamage.remove(player.uniqueId)
        lastMessage.remove(player.uniqueId)
    }

    fun clear() {
        lastDamage.clear()
        lastMessage.clear()
    }

    /** True when the player must not use a gate right now. */
    fun isBlocked(player: Player): Boolean {
        if (player.hasPermission("creativegates.combat.bypass")) return false

        val seconds = CreativeGate.configuration.combatBlockSeconds
        if (seconds > 0) {
            val last = lastDamage[player.uniqueId]
            if (last != null && System.currentTimeMillis() - last < seconds * 1000L) return true
        }
        return PvPManagerHook.isInCombat(player)
    }

    /** Walking inside a portal fires the check for every block, so the message is throttled. */
    fun shouldSendMessage(player: Player): Boolean {
        val now = System.currentTimeMillis()
        val last = lastMessage[player.uniqueId]
        if (last != null && now - last < MESSAGE_THROTTLE_MILLIS) return false
        lastMessage[player.uniqueId] = now
        return true
    }

    private object PvPManagerHook {
        private var resolved = false
        private var failed = false
        private var getInstance: Method? = null
        private var getPlayerManager: Method? = null
        private var getCombatPlayer: Method? = null
        private var isInCombat: Method? = null

        fun isInCombat(player: Player): Boolean {
            if (failed) return false
            val plugin = Bukkit.getPluginManager().getPlugin("PvPManager") ?: return false
            if (!plugin.isEnabled) return false
            try {
                if (!resolved) {
                    val main = plugin.javaClass
                    getInstance = main.getMethod("getInstance")
                    getPlayerManager = main.getMethod("getPlayerManager")
                    val manager = getPlayerManager!!.returnType
                    getCombatPlayer = manager.getMethod("get", Player::class.java)
                    isInCombat = getCombatPlayer!!.returnType.getMethod("isInCombat")
                    resolved = true
                }
                val instance = getInstance!!.invoke(null) ?: return false
                val manager = getPlayerManager!!.invoke(instance) ?: return false
                val combatPlayer = getCombatPlayer!!.invoke(manager, player) ?: return false
                return isInCombat!!.invoke(combatPlayer) == true
            } catch (e: Throwable) {
                // Different PvPManager version: fall back to the damage timer only, and say so once.
                failed = true
                Bukkit.getLogger().log(
                    Level.WARNING,
                    "[CreativeGate] PvPManager combat tag could not be read, using only the damage timer",
                    e
                )
                return false
            }
        }
    }
}
