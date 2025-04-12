package me.santio.npe.data

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import io.netty.buffer.ByteBuf

data class PacketLogData(
    val type: PacketTypeCommon,
    val data: ByteBuf
)