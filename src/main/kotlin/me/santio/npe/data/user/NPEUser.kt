package me.santio.npe.data.user

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.santio.npe.NPE
import me.santio.npe.base.BufferKey
import me.santio.npe.base.Processor
import me.santio.npe.data.PacketLogData
import me.santio.npe.io.BoundedOutputStream
import me.santio.npe.metrics.BStatsMetrics
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.*
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack

data class NPEUser(
    val username: String,
    val uniqueId: UUID,
) {

    lateinit var data: UserData

    private val packetRate: MutableMap<PacketTypeCommon, AtomicLong> = ConcurrentHashMap()
    private var debug: String? = null

    val debounce: MutableMap<String, AlertData> = ConcurrentHashMap()
    val violations: MutableMap<String, Long> = ConcurrentHashMap()
    var lastPacket: PacketLogData? = null
    val logBuffer: BoundedOutputStream = BoundedOutputStream(1048576) // 1MB
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
        sendDebug(NPE.miniMessage.deserialize(message).color(NamedTextColor.GRAY), chat)
    }

    /**
     * Log data to the player buffer
     * @param data the data to log
     */
    fun log(data: String) {
        val time = formatter.format(Date())
        logBuffer.write(("[$time] $data\n").toByteArray())
    }

    /**
     * Send a debug component message to the user
     * @param message the message to send
     */
    fun sendDebug(message: Component, chat: Boolean = false) {
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
     * @param alert the data in the alert
     * @return the flag data
     */
    fun flag(alert: AlertData) {
        this.violations.put(alert.id, this.violations.getOrDefault(alert.id, 0L) + 1)
        val message = alert.component() ?: return

        this.log(plainSerializer.serialize(message) + "\n" + plainSerializer.serialize(alert.hover()) + "\n")
        BStatsMetrics.recordAlert(alert.id)

        if (this.debounce.containsKey(alert.id)) {
            val existing = debounce[alert.id]!!
            existing.count += 1
            existing.data = alert.data
            return
        }

        this.debounce[alert.id] = alert
    }

    fun getPath(): Path {
        return NPE.instance.dataPath.resolve("data").resolve("$uniqueId.json")
    }

    /**
     * Loads the user's settings from the database
     */
    suspend fun load() {
        withContext(Dispatchers.IO) {
            val path = getPath()
            if (!path.parent.exists()) path.parent.createDirectories()

            if (!path.exists()) data = UserData()
            else data = gson.fromJson(path.readText(), UserData::class.java)
        }
    }

    /**
     * Saves the user's settings to the database
     */
    suspend fun save() {
        withContext(Dispatchers.IO) {
            val path = getPath()

            if (!path.parent.exists()) path.parent.createDirectories()
            if (!path.exists() && !data.hasData()) return@withContext // Don't save if there's no data

            if (!path.exists()) path.createFile()

            if (data.hasData()) {
                path.writeText(gson.toJson(data))
            } else {
                path.deleteExisting()
            }
        }
    }

    companion object {
        private val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

        private val plainSerializer = PlainTextComponentSerializer.plainText()
        private val jsonSerializer = JSONComponentSerializer.json();

        private val gson = GsonBuilder()
            .serializeNulls()
            .create()

        /**
         * A holding of all users registered with NPE
         */
        internal val users = ConcurrentHashMap<UUID, NPEUser>()

        /**
         * Get or create a [NPEUser] for the specified [player]
         * @param player the player to get the [NPEUser] for
         * @return the [NPEUser] for the specified [player]
         */
        fun getOrCreate(player: Player): NPEUser {
            val uniqueId = player.uniqueId
            return users.getOrPut(uniqueId) {
                NPEUser(player.name, uniqueId)
            }
        }
    }
}

/**
 * Helper method for getting the [NPEUser] for a [Player]
 * @see NPEUser.getOrCreate
 */
val Player.npe get() = NPEUser.getOrCreate(this)