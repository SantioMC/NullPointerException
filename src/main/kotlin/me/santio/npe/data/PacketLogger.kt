package me.santio.npe.data

import io.netty.buffer.ByteBufUtil
import me.santio.npe.NPE
import me.santio.npe.config.config
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Handles managing packet logging for players and other information
 * @author santio
 */
object PacketLogger {

    fun saveLog(player: NPEUser) {
        if (!config("npe.log-packets.enabled", true)) return

        val hexDump = player.lastPacket?.let {
            ByteBufUtil.hexDump(it.data)
        } ?: "null"

        val violations = player.violations.entries.joinToString("\n") {
            "${it.key} - x${it.value}"
        }.takeIf { it.isNotBlank() } ?: "No violations :)"

        val log = """
            ${player.username}
            ${player.uniqueId}
            
            Violations:
            {violations}
            
            Last Packet Sent:
            {packet}
            
            {hexdump}
            
            Logged Data:
            {data}
        """.trimIndent()
            .replace("{packet}", player.lastPacket?.type?.name ?: "null")
            .replace("{hexdump}", hexDump)
            .replace("{violations}", violations)
            .replace("{data}", player.logBuffer.toByteArray().toString(Charsets.UTF_8))

        val file = NPE.instance.dataPath
            .resolve("logs")
            .resolve("${player.uniqueId}.log")

        if (!file.parent.exists()) file.parent.createDirectories()
        if (!file.exists()) file.createFile()

        file.writeText(log)
    }

}