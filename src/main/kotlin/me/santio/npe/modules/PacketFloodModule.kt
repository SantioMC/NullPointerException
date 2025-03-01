package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor
import me.santio.npe.data.npe
import org.bukkit.entity.Player

@AutoService(Processor::class)
class PacketFloodModule: Module(
    name = "Sending too many packets",
    id = "packet flood"
) {

    private val ignore = listOf(
        PacketType.Play.Client.CLIENT_TICK_END,
        PacketType.Play.Client.PONG,
        PacketType.Play.Client.CREATIVE_INVENTORY_ACTION,
        PacketType.Play.Client.PLAYER_POSITION,
    )

    private val maxIndividualPacket = 30;
    private val maxPackets = 1_000;

    override fun getPacket(event: PacketReceiveEvent) {
        val player = event.getPlayer<Player>()

        // Check if the player is sending too many packets in total
        val pps = player.npe.pps()
        if (pps >= maxPackets) return flag(event) {
            "packetsPerSecond" to pps
            "maxPackets" to maxPackets
        }

        // Check if any individual packets are over the limit
        val isOverLimit = player.npe.pps(event.packetType).get() >= maxIndividualPacket
        if (isOverLimit && !ignore.contains(event.packetType)) {
            val buffer = player.npe.buffer(this, "flood:${event.packetType.name}")
            return flag(event, disconnect = buffer.incrementAndGet() >= 10) {
                "packetType" to event.packetType.name
                "packetsPerSecond" to pps
                "maxPackets" to maxIndividualPacket
            }
        }
    }

}