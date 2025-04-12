package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor
import org.bukkit.entity.Player

@AutoService(Processor::class)
class InventorySlotModule: Module(
    id = "Inventory Slot Crash",
    description = "Sending an invalid slot id to the server",
    config = "inventory-slot"
) {

    override fun filter(): List<PacketTypeCommon>? {
        return listOf(PacketType.Play.Client.CLICK_WINDOW)
    }

    override fun getPacket(event: PacketReceiveEvent) {
        val wrapper = WrapperPlayClientClickWindow(event)
        val player = event.getPlayer<Player>()

        if (wrapper.slot == -1 || wrapper.slot == -999) return

        val maxSlots = player.openInventory.countSlots()
        if (wrapper.slot >= 0 && wrapper.slot <= maxSlots) return

        flag(event) {
            "slot" to wrapper.slot
            "maxSlots" to maxSlots
        }
    }

}