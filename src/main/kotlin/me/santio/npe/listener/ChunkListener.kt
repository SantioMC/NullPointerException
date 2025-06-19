package me.santio.npe.listener

import com.google.auto.service.AutoService
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import me.santio.npe.config.config
import me.santio.npe.data.user.AlertData
import me.santio.npe.data.user.npe
import me.santio.npe.listener.loader.annotations.Toggle
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
@Toggle("modules.chunk-loading.enabled")
class ChunkListener: Listener {

    private val buffer by lazy { config("modules.chunk-loading.buffer", 250) }

    private fun isTooFarOutside(chunk: Chunk, buffer: Int): Boolean {
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
    suspend fun onChunkLoad(event: PlayerChunkLoadEvent) {
        if (!isTooFarOutside(event.chunk, buffer)) return

        val chunkLocX = event.chunk.x shl 4
        val chunkLocZ = event.chunk.z shl 4
        val worldBorder = event.chunk.world.worldBorder
        val borderSize = (worldBorder.size / 2.0) + buffer

        val alert = AlertData(
            event.player,
            "Illegal Chunk Load",
            disconnect = config("modules.chunk-loading.resolution", "kick") == "kick"
        ) {
            "chunkX" to chunkLocX
            "chunkZ" to chunkLocZ
            "worldBorder" to worldBorder.size
            "borderSize (w/ buffer)" to borderSize
        }

        event.player.npe.flag(alert)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @Toggle("modules.chunk-loading.send_back_inside_border", default = true)
    private fun onSpawn(event: PlayerSpawnLocationEvent) {
        if (!isTooFarOutside(event.player.location.chunk, buffer = 0)) return
        event.spawnLocation = constraintLocation(event.spawnLocation)
    }

}