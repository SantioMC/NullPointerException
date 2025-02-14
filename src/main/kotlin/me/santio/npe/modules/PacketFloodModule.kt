package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor
import me.santio.npe.base.ProcessorLoader
import me.santio.npe.processor.RateProcessor

@AutoService(Processor::class)
class PacketFloodModule: Module(
    name = "Sending too many packets",
    id = "packet flood"
) {

    private val maxPackets = 1_000;

    override fun getPacket(event: PacketReceiveEvent) {
        val processor = ProcessorLoader.get<RateProcessor>()
        val pps = processor.getRate(event.user.uuid)

        if (pps >= maxPackets) flag(event)
    }

}