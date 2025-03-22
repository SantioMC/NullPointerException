package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor

@AutoService(Processor::class)
class PacketSizeModule: Module(
    id = "Massive Packets",
    description = "Sending packets that are too large",
    config = "packet-size"
) {

    override fun getPacket(event: PacketReceiveEvent) {
        val buffer = event.byteBuf
        val readable = ByteBufHelper.readableBytes(buffer)

        val maxBytes = if (event.packetType == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            config("max_creative_item_bytes", 1024)
        } else {
            config("max_bytes", 64000)
        }

        if (readable > maxBytes) flag(event) {
            "packetSize" to readable
        }
    }

}