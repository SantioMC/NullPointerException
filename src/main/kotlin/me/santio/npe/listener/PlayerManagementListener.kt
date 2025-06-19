package me.santio.npe.listener

import com.google.auto.service.AutoService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.santio.npe.NPE
import me.santio.npe.data.PacketLogger
import me.santio.npe.data.user.NPEUser
import me.santio.npe.data.user.npe
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.function.Consumer

@Suppress("unused")
@AutoService(Listener::class)
class PlayerManagementListener: Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onQuit(event: PlayerQuitEvent) {
        event.player.npe.save()
        PacketLogger.saveLog(event.player.npe)

        Bukkit.getScheduler().runTaskLater(NPE.instance, Consumer {
            if (event.player.isOnline) return@Consumer
            NPEUser.users.remove(event.player.uniqueId)
        }, 20L)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    suspend fun onJoin(event: PlayerJoinEvent) {
        event.player.npe.load()
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

}