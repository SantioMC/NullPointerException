package me.santio.npe.base

import com.github.retrooper.packetevents.event.PacketListenerPriority

abstract class Module(
    override val id: String,
    val description: String,
    override val config: String,
    override val clone: Boolean = true,
    override val priority: PacketListenerPriority = PacketListenerPriority.NORMAL,
): Processor(id, config, clone, priority)