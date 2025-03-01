package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor

@AutoService(Processor::class)
class PacketSizeModule: Module(
    name = "Packet size too large",
    id = "Massive Packets"
) {

    private val maxBytes = 64_000;

    override fun getPacket(event: PacketReceiveEvent) {
        val buffer = event.byteBuf
        val readable = ByteBufHelper.readableBytes(buffer)

        if (readable > maxBytes) flag(event, disconnect = false) {
            "packetSize" to readable
        }
    }

}