package me.santio.npe.processor

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.google.auto.service.AutoService
import me.santio.npe.base.Processor
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@AutoService(Processor::class)
class RateProcessor: Processor("Packet Rate") {
    private val pps: MutableMap<UUID, AtomicLong> = ConcurrentHashMap()

    override fun getPacket(event: PacketReceiveEvent) {
        pps.getOrPut(event.user.uuid) {
            AtomicLong(0)
        }.getAndIncrement()

        scheduler.schedule({
            if (pps[event.user.uuid]?.get() == 1L) {
                pps.remove(event.user.uuid)
            } else if (pps.containsKey(event.user.uuid)) {
                pps[event.user.uuid]?.decrementAndGet()
            }
        }, 1, TimeUnit.SECONDS)
    }

    fun getRate(uniqueId: UUID): Long {
        return pps.getOrDefault(uniqueId, AtomicLong(0)).get()
    }
}
