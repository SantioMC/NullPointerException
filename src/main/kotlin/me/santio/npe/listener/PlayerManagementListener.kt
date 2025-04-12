package me.santio.npe.listener

import com.google.auto.service.AutoService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.santio.npe.NPE
import me.santio.npe.data.NPEUser
import me.santio.npe.data.PacketLogger
import me.santio.npe.data.npe
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
    private fun onQuit(event: PlayerQuitEvent) {
        scope.launch {
            event.player.npe.save()
            PacketLogger.saveLog(event.player.npe)

            Bukkit.getScheduler().runTaskLater(NPE.instance, Consumer {
                if (event.player.isOnline) return@Consumer
                NPEUser.users.remove(event.player.uniqueId)
            }, 20L)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onJoin(event: PlayerJoinEvent) {
        scope.launch {
            event.player.npe.load()
        }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

}