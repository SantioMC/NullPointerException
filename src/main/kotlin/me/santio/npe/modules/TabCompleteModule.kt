package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete
import com.google.auto.service.AutoService
import me.santio.npe.base.Module
import me.santio.npe.base.Processor

@AutoService(Processor::class)
class TabCompleteModule: Module(
    id = "Tab Complete",
    description = "Tab completing a command argument with expensive data",
    config = "tab-complete"
) {

    override fun filter(): List<PacketTypeCommon>? {
        return listOf(PacketType.Play.Client.TAB_COMPLETE)
    }

    override fun getPacket(event: PacketReceiveEvent) {
        val wrapper = WrapperPlayClientTabComplete(event)

        val maxCommandLength = config("max_command_length", 256)
        val maxSquareBrackets = config("max_square_brackets", 10)
        val maxCurlyBrackets = config("max_curly_brackets", 15)

        val squareBrackets = wrapper.text.count { it == '[' }
        val curlyBrackets = wrapper.text.count { it == '{' }

        if (squareBrackets > maxSquareBrackets) {
            flag(event) {
                "text" to wrapper.text
                "length" to wrapper.text.length
                "character" to '['
                "maxLength" to maxSquareBrackets
            }
        } else if (curlyBrackets > maxCurlyBrackets) {
            flag(event) {
                "text" to wrapper.text
                "length" to wrapper.text.length
                "character" to '{'
                "maxLength" to maxCurlyBrackets
            }
        } else if (wrapper.text.length > maxCommandLength) {
            flag(event) {
                "text" to wrapper.text
                "length" to wrapper.text.length
                "maxLength" to maxCommandLength
            }
        }
    }

}