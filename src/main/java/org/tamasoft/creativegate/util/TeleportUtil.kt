package org.tamasoft.creativegate.util

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.tamasoft.creativegate.CreativeGate
import org.tamasoft.creativegate.teleport.Destination
import org.tamasoft.creativegate.teleport.TeleporterException
import java.util.EnumSet
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object TeleportUtil {

    // Players whose gate teleport is still in flight. Without this, standing in the portal while the
    // destination chunk loads would start a second teleport.
    private val inFlight: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    fun begin(player: Player): Boolean = inFlight.add(player.uniqueId)

    fun end(player: Player) {
        inFlight.remove(player.uniqueId)
    }

    fun clear() {
        inFlight.clear()
    }

    /**
     * Resolves the exit location: centre of the stored block (the old code used the block corner, which put the
     * hitbox into up to four neighbouring blocks) with the stored heading. Touches no blocks and loads no chunk.
     */
    @Throws(TeleporterException::class)
    fun resolveLocation(destination: Destination): Location {
        try {
            val world = destination.getBukkitWorld()
            return Location(
                world,
                destination.location.blockX + 0.5,
                destination.location.blockY.toDouble(),
                destination.location.blockZ + 0.5,
                destination.heading.yaw,
                destination.heading.pitch
            )
        } catch (e: Exception) {
            throw TeleporterException(TxtUtil.parse("<b>Could not calculate the location: ${e.message}"))
        }
    }

    private val DANGEROUS: Set<Material> = EnumSet.of(
        Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK,
        Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.POWDER_SNOW, Material.CACTUS,
        Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE
    )

    /**
     * The exit is the spot where the creator stood, and anyone can change it afterwards. Feet and head must be
     * passable and nothing at the feet, the head or below may burn or hurt (lava is passable, so the old
     * passable-only check let an exit lead straight into it). Only call with the chunk loaded.
     */
    fun isSafeExit(location: Location): Boolean {
        val feet = location.block
        val head = feet.getRelative(BlockFace.UP)
        val below = feet.getRelative(BlockFace.DOWN)
        if (!feet.isPassable || !head.isPassable) return false
        return feet.type !in DANGEROUS && head.type !in DANGEROUS && below.type !in DANGEROUS
    }

    /**
     * Starts the teleport. The returned future completes with true once the player has arrived and with false when
     * the exit is blocked or dangerous or the teleport was cancelled (for example by another plugin). The exit is
     * checked only after its chunk has loaded; the old check skipped unloaded chunks completely.
     */
    @Throws(TeleporterException::class)
    fun teleport(teleportee: Player, destination: Destination): CompletableFuture<Boolean> {
        val location = resolveLocation(destination)

        return location.world.getChunkAtAsync(location).thenCompose { _ ->
            if (!teleportee.isOnline || teleportee.isDead) {
                return@thenCompose CompletableFuture.completedFuture(false)
            }
            if (!isSafeExit(location)) {
                TxtUtil.sendTranslation(teleportee, CreativeGate.configuration.exitBlockedMessage)
                return@thenCompose CompletableFuture.completedFuture(false)
            }

            // Eject passengers and unmount before transport.
            teleportee.eject()
            teleportee.vehicle?.eject()

            teleportee.teleportAsync(location, PlayerTeleportEvent.TeleportCause.PLUGIN)
        }
    }
}
