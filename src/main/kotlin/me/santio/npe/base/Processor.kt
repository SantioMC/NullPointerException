package me.santio.npe.base

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import me.santio.npe.NPE
import me.santio.npe.data.npe
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.entity.Player
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory

abstract class Processor(
    open val id: String,
    open val config: String,
    open val priority: PacketListenerPriority = PacketListenerPriority.LOWEST
): PacketListener {

    final override fun onPacketReceive(event: PacketReceiveEvent?) {
        if (!this@Processor.config("enabled", true)) return
        if (event?.user?.profile == null || event.user?.uuid == null || event.getPlayer<Player>() == null) return

        if (filter()?.contains(event.packetType) != false) {
            try {
                this.getPacket(event)
            } catch (e: Exception) {
                NPE.logger.error("Failed to handle receiving packet", e)
                NPE.logger.warn("The player '${event.user.name}' was disconnected to prevent corrupted state")

                event.isCancelled = true
                event.markForReEncode(true)

                flag(event) {
                    "exception" to e.javaClass.simpleName
                    "message" to e.message
                }
            }
        }
    }

    final override fun onPacketSend(event: PacketSendEvent?) {
        if (!this@Processor.config("enabled", true)) return
        if (event?.user?.profile == null || event.user?.uuid == null || event.getPlayer<Player>() == null) return

        if (filter()?.contains(event.packetType) != false) {
            try {
                this.sendPacket(event)
            } catch (e: Exception) {
                NPE.logger.error("Failed to handle sending packet", e)
            }
        }
    }

    open fun getPacket(event: PacketReceiveEvent) {}
    open fun sendPacket(event: PacketSendEvent) {}

    open fun filter(): List<PacketTypeCommon>? {
        return null
    }

    protected fun resolution(default: Resolution = Resolution.KICK): Resolution {
        return this@Processor.config("resolution", default)
    }

    open fun flag(
        event: PacketReceiveEvent,
        resolution: Resolution = Resolution.KICK,
        clickEvent: ClickEvent? = null,
        extra: Component? = null,
        data: FlagData.() -> Unit = {}
    ) {
        val player = event.getPlayer<Player>()
        if (player == null) return

        val resolution = resolution(resolution)
        if (resolution.shouldCancel && !player.npe.bypassing()) event.isCancelled = true

        event.getPlayer<Player>().npe.flag(
            event.getPlayer<Player>(),
            id,
            resolution.shouldKick,
            clickEvent,
            extra
        ) {
            "packetType" to event.packetType.name
            "resolution" to resolution.name.lowercase()
            data()
        }
    }

    override fun toString(): String {
        return id
    }

    //region Config Helpers
    fun config(key: String): String? {
        return me.santio.npe.config.config("modules.$config.$key")
    }

    fun config(key: String, default: String): String {
        return me.santio.npe.config.config("modules.$config.$key", default)
    }

    inline fun <reified T: Any> config(key: String, default: T): T {
        return me.santio.npe.config.config<T>("modules.$config.$key", default)
    }
    //endregion

    protected companion object {
        val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2, ThreadFactory { r ->
            Thread(r, "NPE-Scheduler")
        })
    }

}