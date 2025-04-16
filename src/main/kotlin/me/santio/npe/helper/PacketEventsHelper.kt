package me.santio.npe.helper

import com.github.retrooper.packetevents.event.ProtocolPacketEvent
import com.github.retrooper.packetevents.resources.ResourceLocation
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.kyori.adventure.key.Key

fun ResourceLocation.toKey(): Key {
    return Key.key(this.namespace, this.key)
}

val ProtocolPacketEvent.buffer: ByteBuf
    get() = this.byteBuf as ByteBuf

fun ByteBuf.heapCopy(): ByteBuf {
    return Unpooled.copiedBuffer(this)
}