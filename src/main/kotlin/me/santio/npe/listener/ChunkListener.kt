package me.santio.npe.listener

import com.google.auto.service.AutoService
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import me.santio.npe.data.npe
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
@AutoService(Listener::class)
class ChunkListener: Listener {

    private fun isTooFarOutside(chunk: Chunk, buffer: Int = BORDER_BUFFER): Boolean {
        val worldBorder = chunk.world.worldBorder
        val borderSize = (worldBorder.size / 2.0) + buffer

        val chunkLocX = abs(chunk.x shl 4)
        val chunkLocZ = abs(chunk.z shl 4)

        return chunkLocX > borderSize || chunkLocZ > borderSize
    }

    private fun constraintLocation(location: Location): Location {
        val worldBorder = location.world.worldBorder
        val borderSize = (worldBorder.size / 2.0)

        val x = max(borderSize, min(location.x, -borderSize))
        val z = max(borderSize, min(location.z, -borderSize))

        return location.clone().apply {
            this.x = x - 2
            this.z = z - 2
        }.toHighestLocation().add(0.0, 1.0, 0.0)
    }

    @EventHandler(priority = EventPriority.LOW)
    private fun onChunkLoad(event: PlayerChunkLoadEvent) {
        if (!isTooFarOutside(event.chunk)) return

        val chunkLocX = event.chunk.x shl 4
        val chunkLocZ = event.chunk.z shl 4
        val worldBorder = event.chunk.world.worldBorder
        val borderSize = (worldBorder.size / 2.0) + BORDER_BUFFER

        event.player.npe.flag(
            event.player,
            "illegal chunk load",
        ) {
            "chunkX" to chunkLocX
            "chunkZ" to chunkLocZ
            "worldBorder" to worldBorder.size
            "borderSize (w/ buffer)" to borderSize
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onSpawn(event: PlayerSpawnLocationEvent) {
        if (!isTooFarOutside(event.player.location.chunk, buffer = 0)) return
        event.spawnLocation = constraintLocation(event.spawnLocation)
    }

    companion object {
        private const val BORDER_BUFFER = 250
    }

}