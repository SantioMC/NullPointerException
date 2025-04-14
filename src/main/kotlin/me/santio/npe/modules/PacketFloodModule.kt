package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor
import me.santio.npe.data.npe
import org.bukkit.entity.Player

@AutoService(Processor::class)
class PacketFloodModule: Module(
    id = "Packet Flood",
    description = "Sending too many packets in a short period of time",
    config = "packet-flood"
) {

    private val ignore = listOf(
        PacketType.Play.Client.CLIENT_TICK_END,
        PacketType.Play.Client.PONG,
    )

    override fun getPacket(event: PacketReceiveEvent) {
        val player = event.getPlayer<Player>()

        var maxPacketsPerSecond = config("max_packets_per_second", 30)
        val floodDuration = config("flood_window", 3)
        val hardLimitPerSecond = config("hard_limit_per_second", 1000)

        // Check if the player is sending too many packets in total
        val pps = player.npe.pps()
        if (pps >= hardLimitPerSecond) return flag(event) {
            "packetsPerSecond" to pps
            "maxPackets" to hardLimitPerSecond
            "limit" to "hard"
        }

        // Handle creative item flood when just clearing the inventory
        if (event.packetType == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            val wrapper = try {
                WrapperPlayClientCreativeInventoryAction(event)
            } catch (_: Exception) { null }

            if (wrapper != null && wrapper.itemStack.isEmpty) {
                // We want to increase the buffer before we flag the packet
                maxPacketsPerSecond = config("max_empty_creative_item_packets_per_second", 920)
            }
        } else if (event.packetType == PacketType.Play.Client.TAB_COMPLETE) {
            maxPacketsPerSecond = config("max_tab_complete_packets_per_second", 60)
        }

        // Check if any individual packets are over the limit
        val isOverLimit = player.npe.pps(event.packetType).get() >= maxPacketsPerSecond
        if (isOverLimit && !ignore.contains(event.packetType)) {
            val buffer = player.npe.buffer(this, "flood:${event.packetType.name}")

            if (buffer.incrementAndGet() >= floodDuration) {
                buffer.set(0)
                return flag(event) {
                    "packetType" to event.packetType.name
                    "packetsPerSecond" to pps
                    "limit" to "per_packet"
                    "maxPackets" to maxPacketsPerSecond
                }
            }
        }
    }

}