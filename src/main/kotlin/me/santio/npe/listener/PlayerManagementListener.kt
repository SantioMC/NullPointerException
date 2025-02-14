package me.santio.npe.listener

import com.google.auto.service.AutoService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.santio.npe.data.NPEUser
import me.santio.npe.data.npe
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@AutoService(Listener::class)
class PlayerManagementListener: Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onQuit(event: PlayerQuitEvent) {
        scope.launch {
            event.player.npe.save()
            NPEUser.users.remove(event.player.uniqueId)
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