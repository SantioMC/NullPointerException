package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

@AutoService(Processor::class)
class LecternModule: Module(
    id = "Lectern Click",
    description = "Clicking in a lectern GUI is not possible",
    config = "lectern-click"
) {

    override fun filter(): List<PacketTypeCommon>? {
        return listOf(PacketType.Play.Client.CLICK_WINDOW)
    }

    override fun getPacket(event: PacketReceiveEvent) {
        val player = event.getPlayer<Player>()
        if (player.openInventory.topInventory.type != InventoryType.LECTERN) return

        flag(event) {
            "type" to player.openInventory.topInventory.type
        }
    }

}