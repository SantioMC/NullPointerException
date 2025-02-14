package me.santio.npe.processor

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.google.auto.service.AutoService
import me.santio.npe.base.Processor
import me.santio.npe.data.PacketDumper

@AutoService(Processor::class)
class LogProcessor: Processor("Logger") {

    override fun filter(): List<PacketTypeCommon>? {
        return listOf(
            PacketType.Play.Client.CREATIVE_INVENTORY_ACTION
        )
    }

    override fun getPacket(event: PacketReceiveEvent) {
        PacketDumper.dump(event)
    }

}
