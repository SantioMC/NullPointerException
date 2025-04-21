package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSelectBundleItem
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor

@AutoService(Processor::class)
class BundleItemModule: Module(
    id = "Bundle Item Bounds",
    description = "Detection against illegally setting your bundle slot to a negative number",
    config = "bundle-item"
) {

    override fun filter(): List<PacketTypeCommon>? {
        return listOf(PacketType.Play.Client.SELECT_BUNDLE_ITEM)
    }

    override fun getPacket(event: PacketReceiveEvent) {
        val wrapper = WrapperPlayClientSelectBundleItem(event)
        if (wrapper.slotId >= 0 && wrapper.selectedItemIndex >= -1) return

        flag(event) {
            "slotId" to wrapper.slotId
            "selectedItemIndex" to wrapper.selectedItemIndex
        }
    }

}