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
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer
import org.bukkit.entity.Player
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory

abstract class Processor(
    open val id: String,
    open val clone: Boolean = true,
    open val priority: PacketListenerPriority = PacketListenerPriority.LOWEST
): PacketListener {

    private val serializer = JSONComponentSerializer.json();

    final override fun onPacketReceive(event: PacketReceiveEvent?) {
        if (event?.user?.profile == null || event.user?.uuid == null) return

        if (filter()?.contains(event.packetType) != false) {
            val event = if (clone) event.clone() else event
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
        if (event?.user?.profile == null || event.user?.uuid == null) return

        if (filter()?.contains(event.packetType) != false) {
            val event = if (clone) event.clone() else event
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

    open fun flag(event: PacketReceiveEvent, disconnect: Boolean = true, clickEvent: ClickEvent? = null, extra: Component? = null, data: FlagData.() -> Unit = {}) {
        val details = FlagData(mutableMapOf(
            "packetType" to event.packetType.name
        )).apply(data)

        // Permission check
        try {
            val player = event.getPlayer<Player>()
            if (!player.npe.bypassing() && disconnect) {
                event.user.closeConnection()
            }
        } catch (e: Exception) {
            NPE.logger.error("Failed to perform permission check on '{}'", event.user.name, e)
        }

        // Send the alert to administrators
        var hover = Component.newline()
        for (data in details.map()) {
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
            .append(Component.text(event.getPlayer<Player>().name, NamedTextColor.RED))
            .append(Component.text(" was detected for ", NamedTextColor.GRAY))
            .append(Component.text(this.id, NamedTextColor.YELLOW))
            .hoverEvent(hover)
            .clickEvent(clickEvent)

        NPE.broadcast(alert)
    }

    protected companion object {
        val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2, ThreadFactory { r ->
            Thread(r, "NPE-Scheduler")
        })
    }

}