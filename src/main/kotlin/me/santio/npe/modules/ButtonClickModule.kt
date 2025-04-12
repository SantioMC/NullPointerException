package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor

@AutoService(Processor::class)
class ButtonClickModule: Module(
    id = "Invalid Button Click",
    description = "Clicking with a button key that isn't possible to use",
    config = "button-click"
) {

    override fun filter(): List<PacketTypeCommon>? {
        return listOf(PacketType.Play.Client.CLICK_WINDOW)
    }

    override fun getPacket(event: PacketReceiveEvent) {
        val wrapper = WrapperPlayClientClickWindow(event)
        if (wrapper.button >= 0 && wrapper.button <= 40) return

        flag(event) {
            "button" to wrapper.button
        }
    }

}