package me.santio.npe.processor

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.google.auto.service.AutoService
import me.santio.npe.base.Processor
import me.santio.npe.data.NPEUser
import me.santio.npe.helper.not
import me.santio.npe.inspection.PacketInspection
import org.bukkit.entity.Player

@Suppress("DuplicatedCode")
@AutoService(Processor::class)
class LogProcessor: Processor("Logger", "log-processor") {

    private val sendComponent = !"<#8cd17d>\uD83E\uDC18 "
    private val receiveComponent = !"<#d1847d>\uD83E\uDC1A "

    private val spamPackets: Set<PacketTypeCommon> = setOf(
        PacketType.Play.Server.SYSTEM_CHAT_MESSAGE,
        PacketType.Play.Client.PLAYER_POSITION,
        PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
        PacketType.Play.Client.PLAYER_ROTATION,
        PacketType.Play.Client.KEEP_ALIVE,
        PacketType.Play.Client.PLAYER_INPUT,
        PacketType.Play.Client.ENTITY_ACTION,
        PacketType.Play.Client.TAB_COMPLETE,
        PacketType.Play.Client.PONG,
        PacketType.Play.Server.PING,
        PacketType.Play.Server.CHUNK_DATA,
        PacketType.Play.Server.ENTITY_POSITION_SYNC,
        PacketType.Play.Server.ENTITY_HEAD_LOOK,
        PacketType.Play.Server.UPDATE_HEALTH,
        PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION,
        PacketType.Play.Server.ENTITY_RELATIVE_MOVE,
        PacketType.Play.Server.TAB_COMPLETE,
        PacketType.Play.Server.ENTITY_VELOCITY,
        PacketType.Play.Server.TIME_UPDATE,
        PacketType.Play.Server.PLAYER_INFO_UPDATE,
        PacketType.Play.Server.UNLOAD_CHUNK,
        PacketType.Play.Server.UPDATE_VIEW_POSITION,
        PacketType.Play.Server.BUNDLE,
        PacketType.Play.Server.KEEP_ALIVE,
        PacketType.Play.Server.ENTITY_ROTATION,
        PacketType.Play.Server.ENTITY_METADATA,
        PacketType.Play.Server.UPDATE_ATTRIBUTES,
        PacketType.Play.Server.DESTROY_ENTITIES,
        PacketType.Play.Server.SPAWN_ENTITY,
        PacketType.Play.Server.ENTITY_EQUIPMENT,
        PacketType.Play.Server.ENTITY_STATUS,
        PacketType.Play.Server.DAMAGE_EVENT,
        PacketType.Play.Server.UPDATE_LIGHT,
        PacketType.Play.Server.SOUND_EFFECT
    )

    override fun getPacket(event: PacketReceiveEvent) {
        if (event.packetType in spamPackets) return
        if (NPEUser.users.none { it.value.debugging("packets") || it.value.debugging("self-packets") }) return

        val component = PacketInspection.inspect(event) ?: return

        for (user in NPEUser.users.values) {
            val isSelf = event.getPlayer<Player>() == user.player()
            val debugging = user.debugging("packets") || (user.debugging("self-packets") && isSelf)

            if (debugging) {
                user.sendDebug(receiveComponent.append(component), chat = true)
            }
        }
    }

    override fun sendPacket(event: PacketSendEvent) {
        if (event.packetType in spamPackets) return
        if (NPEUser.users.none { it.value.debugging("packets") || it.value.debugging("self-packets") }) return

        val component = PacketInspection.inspect(event) ?: return

        for (user in NPEUser.users.values) {
            val isSelf = event.getPlayer<Player>() == user.player()
            val debugging = user.debugging("packets") || (user.debugging("self-packets") && isSelf)

            if (debugging) {
                user.sendDebug(sendComponent.append(component), chat = true)
            }
        }
    }
}
