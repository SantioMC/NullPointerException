package me.santio.npe.processor

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.google.auto.service.AutoService
import me.santio.npe.base.Processor
import me.santio.npe.data.npe
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit

@AutoService(Processor::class)
class RateProcessor: Processor("Packet Rate", "packet-rate") {
    override fun getPacket(event: PacketReceiveEvent) {
        val player = event.getPlayer<Player>()
        val pps = player.npe.pps(event.packetType).incrementAndGet()
        sendDebug(player, pps)

        scheduler.schedule({
            val value = player.npe.pps(event.packetType)
            val pps = value.decrementAndGet()
            sendDebug(player, pps)
        }, 1, TimeUnit.SECONDS)
    }

    private fun sendDebug(player: Player, pps: Long) {
        if (player.npe.debugging("pps")) {
            player.npe.sendDebug("Packets per Second: <debug>$pps")
        }
    }
}
