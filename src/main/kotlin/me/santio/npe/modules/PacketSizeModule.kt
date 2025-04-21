package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor
import me.santio.npe.base.Resolution
import me.santio.npe.data.user.npe
import me.santio.npe.helper.buffer
import org.bukkit.entity.Player

@AutoService(Processor::class)
class PacketSizeModule: Module(
    id = "Massive Packets",
    description = "Sending packets that are too large",
    config = "packet-size"
) {

    override fun getPacket(event: PacketReceiveEvent) {
        val readable = event.buffer.readableBytes()
        val isItemPacket = event.packetType == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION

        val maxBytes = if (isItemPacket) {
            config("max_creative_item_bytes", 1024)
        } else {
            config("max_bytes", 64000)
        }

        if (readable > maxBytes) {
            flag(event) {
                "packetSize" to readable
            }

            // Notify the client if the packet is creative inventory action and the resolution is cancel
            if (
                isItemPacket
                && resolution() == Resolution.CANCEL
                && config("notify_client_on_item_cancel", true)
                && !event.getPlayer<Player>().npe.bypassing()
            ) {
                val packet = WrapperPlayClientCreativeInventoryAction(event)
                val wrapper = WrapperPlayServerSetSlot(
                    0,
                    0,
                    packet.slot,
                    ItemStack.EMPTY
                )

                event.user.sendPacket(wrapper)
            }
        }
    }

}