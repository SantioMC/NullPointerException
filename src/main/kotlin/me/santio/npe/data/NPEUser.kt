package me.santio.npe.data

import me.santio.npe.NPE
import me.santio.npe.database.UserData
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack

data class NPEUser(
    val uniqueId: UUID,
) {

    lateinit var data: UserData

    var creativeCursorSlot: PacketItemStack? = null

    fun bypassing(): Boolean {
        return player()?.hasPermission("npe.bypass") == true && !data.ignoreBypass
    }

    fun alert(message: Component) {
        if (!::data.isInitialized) return

        val player = this.player()
        if (player == null || !data.alerts || !player.hasPermission("npe.alerts")) return

        player.sendMessage(Component.text("♦ ", NPE.primaryColor).append(message))
    }

    fun player(): Player? {
        return Bukkit.getPlayer(this.uniqueId)
    }

    suspend fun load() {
        data = NPE.database.prepare(
            "SELECT * FROM user_settings WHERE id = ?",
            uniqueId.toString()
        ).singleNullable() ?: UserData(uniqueId)
    }

    suspend fun save() {
        NPE.database.prepare(
            "REPLACE INTO user_settings(id, alerts, ignore_bypass) VALUES (:uuid, :alerts, :ignoreBypass)",
            data.bindings()
        )
    }

    companion object {
        internal val users = mutableMapOf<UUID, NPEUser>()

        fun getOrCreate(player: Player): NPEUser {
            val uniqueId = player.uniqueId
            return users.getOrPut(uniqueId) {
                NPEUser(uniqueId)
            }
        }
    }
}

val Player.npe get() = NPEUser.getOrCreate(this)