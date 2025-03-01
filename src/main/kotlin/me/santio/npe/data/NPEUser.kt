package me.santio.npe.data

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import me.santio.npe.NPE
import me.santio.npe.base.BufferKey
import me.santio.npe.base.FlagData
import me.santio.npe.base.Processor
import me.santio.npe.database.UserData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack

data class NPEUser(
    val uniqueId: UUID,
) {

    lateinit var data: UserData

    private val packetRate: MutableMap<PacketTypeCommon, AtomicLong> = ConcurrentHashMap()
    private var debug: String? = null

    val buffer: MutableMap<BufferKey, AtomicLong> = ConcurrentHashMap()
    var creativeCursorSlot: PacketItemStack? = null

    /**
     * Get the buffer for a certain check
     * @return the buffer for a certain check
     */
    fun buffer(processor: Processor, key: String): AtomicLong {
        val key = BufferKey(processor, key)

        if (debug == key.toString()) {
            sendDebug("$key: ${buffer.getOrPut(key) { AtomicLong(0) }.get()}")
        }

        return buffer.getOrPut(key) { AtomicLong(0) }
    }

    /**
     * Set the debugging state for the user
     * @param debug the debugging state
     */
    fun debug(debug: String?) {
        this.debug = debug
    }

    /**
     * Checks if the player is currently debugging a specific value
     * @param key the key to check
     * @return `true` if the player is debugging the specified key, `false` otherwise
     */
    fun debugging(key: String): Boolean {
        return debug == key
    }

    /**
     * Send a debug message to the user
     * @param message the message to send
     */
    fun sendDebug(message: String, chat: Boolean = false) {
        val message = NPE.miniMessage.deserialize(message).color(NamedTextColor.GRAY)
        val component = Component.text("\uD83E\uDDEA ", NPE.debugColor).append(message)

        if (chat) player()?.sendMessage(component)
        else player()?.sendActionBar(component)
    }

    /**
     * Get the number of packets per second for the specified [type]
     * @return the number of packets per second as an [AtomicLong]
     */
    fun pps(type: PacketTypeCommon): AtomicLong {
        return packetRate.getOrPut(type) { AtomicLong(0) }
    }

    /**
     * Get the total number of packets per second across all packet types
     * @return the total number of packets per second as a [Long]
     */
    fun pps(): Long {
        return packetRate.values.sumOf { it.get() }
    }

    /**
     * Checks if the player is currently bypassing NPE protection
     * @return `true` if the player is bypassing, `false` otherwise
     */
    fun bypassing(): Boolean {
        return player()?.hasPermission("npe.bypass") == true && !data.ignoreBypass
    }

    /**
     * Notifies the player of an alert, if the player is not yet initialized fully, the alert will be dropped
     * for the player
     * @param message the message to send to the player
     */
    fun alert(message: Component) {
        if (!::data.isInitialized) return

        val player = this.player()
        if (player == null || !data.alerts || !player.hasPermission("npe.alerts")) return

        player.sendMessage(Component.text("♦ ", NPE.primaryColor).append(message))
    }

    /**
     * Gets the [Player] associated with this user
     * @return the [Player] associated with this user, or `null` if the player is not online
     */
    fun player(): Player? {
        return Bukkit.getPlayer(this.uniqueId)
    }

    /**
     * Sends a flag to the player
     * @param player the player to send the flag to
     * @param id the id of the flag
     * @param disconnect whether to disconnect the player if the flag is clicked
     * @param clickEvent the click event to send to the player
     * @param extra the extra component to send to the player
     * @param data the data to send to the player
     * @return the flag data
     */
    fun flag(
        player: Player,
        id: String,
        disconnect: Boolean = true,
        clickEvent: ClickEvent? = null,
        extra: Component? = null,
        data: FlagData.() -> Unit = {}
    ) {
        val user = PacketEvents.getAPI().playerManager.getUser(player)
        if (user == null) return

        val flagData = FlagData().apply(data)

        // Permission check
        try {
            if (!player.npe.bypassing() && disconnect) {
                user.closeConnection()
            }
        } catch (e: Exception) {
            NPE.logger.error("Failed to perform permission check on '{}'", player.name, e)
        }

        // Send the alert to administrators
        var hover = Component.newline()
        for (data in flagData.map()) {
            hover = hover.append(
                Component.text(data.key, NPE.primaryColor)
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(data.value.toString(), NamedTextColor.WHITE))
                    .appendNewline()
            )
        }

        extra?.takeIf {
            serializer.serialize(it).length <= 2048
        }?.let { hover = hover.append(Component.newline().append(it)) }

        val alert = Component.empty()
            .append(Component.text(player.name, NamedTextColor.RED))
            .append(Component.text(" was detected for ", NamedTextColor.GRAY))
            .append(Component.text(id, NamedTextColor.YELLOW))
            .hoverEvent(hover)
            .clickEvent(clickEvent)

        NPE.broadcast(alert)
    }

    /**
     * Loads the user's settings from the database
     */
    suspend fun load() {
        try {
            data = NPE.database.prepare(
                "SELECT * FROM user_settings WHERE id = ?",
                uniqueId.toString()
            ).singleNullable() ?: UserData(uniqueId)
        } catch (e: Exception) {
            NPE.logger.error("Failed to load user data for '{}'", uniqueId, e)
        }
    }

    /**
     * Saves the user's settings to the database
     */
    suspend fun save() {
        NPE.database.prepare(
            "REPLACE INTO user_settings(id, alerts, ignore_bypass) VALUES (:uuid, :alerts, :ignoreBypass)",
            data.bindings()
        )
    }

    companion object {
        private val serializer = JSONComponentSerializer.json();

        /**
         * A holding of all users registered with NPE
         */
        internal val users = mutableMapOf<UUID, NPEUser>()

        /**
         * Get or create a [NPEUser] for the specified [player]
         * @param player the player to get the [NPEUser] for
         * @return the [NPEUser] for the specified [player]
         */
        fun getOrCreate(player: Player): NPEUser {
            val uniqueId = player.uniqueId
            return users.getOrPut(uniqueId) {
                NPEUser(uniqueId)
            }
        }
    }
}

/**
 * Helper method for getting the [NPEUser] for a [Player]
 * @see NPEUser.getOrCreate
 */
val Player.npe get() = NPEUser.getOrCreate(this)