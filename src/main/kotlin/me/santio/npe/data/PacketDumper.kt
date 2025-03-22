package me.santio.npe.data

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import me.santio.npe.NPE
import me.santio.npe.inspection.PacketInspection

/**
 * Dumps packets received from players to a log file
 * @author santio
 */
object PacketDumper {

    private val dump = NPE.instance.dataFolder.resolve("packet_dump.log")

    fun create() {
        if (!dump.exists()) dump.createNewFile()
    }

    fun dump(packet: PacketReceiveEvent) {
        val player = packet.user.name
        val wrapper = PacketInspection.getWrapper(packet, packet.packetType) ?: return
        val data = PacketInspection.getData(player, wrapper)

        dump.appendText("$player: ${wrapper.javaClass.simpleName}\n")
        dump.appendText(data.entries.joinToString("\n") {
            "${it.key}: ${it.value}"
        })

        val lines = dump.readLines()
        if (lines.size > 10000) {
            val truncated = lines.takeLast(5000)
            dump.writeText(truncated.joinToString("\n"))
        }
    }

}