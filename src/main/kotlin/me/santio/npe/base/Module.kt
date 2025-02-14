package me.santio.npe.base

import com.github.retrooper.packetevents.event.PacketListenerPriority

abstract class Module(
    val name: String,
    override val id: String,
    override val priority: PacketListenerPriority = PacketListenerPriority.NORMAL,
): Processor(id)