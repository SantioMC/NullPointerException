package me.santio.npe.modules

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.google.auto.service.AutoService
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import me.santio.npe.base.Module
import me.santio.npe.base.Processor
import me.santio.npe.base.Resolution
import me.santio.npe.data.npe
import me.santio.npe.inspection.PacketInspection
import me.santio.npe.ruleset.RuleSet
import me.santio.npe.ruleset.item.GenericItemRule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

@AutoService(Processor::class)
class CreativeItemModule: Module(
    id = "Illegal Items",
    description = "Spawning in irregular items",
    config = "illegal-items",
    clone = false
) {

    override fun filter(): List<PacketTypeCommon> {
        return listOf(
            PacketType.Play.Client.CREATIVE_INVENTORY_ACTION
        )
    }

    private fun <T: Any>copyComponent(original: ItemStack, new: ItemStack, rule: GenericItemRule<T>): Boolean {
        val originalComponent = original.getComponent(rule.componentType)
        if (originalComponent.isPresent) {
            if (!rule.config(this, "enabled", true)) {
                // Skip rule, just copy the component
                new.setComponent(rule.componentType, originalComponent.get())
                return true
            }

            // only copy if it matches rule
            if (!rule.check(this, originalComponent.get())) {
                // See if we can correct it
                val corrected = rule.correct(this, originalComponent.get()) ?: return false
                new.setComponent(rule.componentType, corrected)
                return false
            } else {
                // All good, just copy it
                new.setComponent(rule.componentType, originalComponent.get())
            }
        }

        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getPacket(event: PacketReceiveEvent) {
        val buffer = event.fullBufferClone
        val wrapper = WrapperPlayClientCreativeInventoryAction(event)

        val item = wrapper.itemStack
        val player = event.getPlayer<Player>()

        if (player.npe.debugging("item-spawns")) {
            player.npe.sendDebug(PacketInspection.readItem(item.copy()), chat = true)
        }

        // If the value is null, it might be that the player is holding the item in their cursor
        if (item.isEmpty) {
            val item = player.inventory.getItem(toSpigotSlot(wrapper.slot)) ?: return
            player.npe.creativeCursorSlot = SpigotReflectionUtil.decodeBukkitItemStack(item)
            return
        }

        // Ignore if the player already owns this item
        val bukkitItem = SpigotReflectionUtil.encodeBukkitItemStack(item)
        val inventoryHasItem = player.inventory.contents.filterNotNull().any { it == bukkitItem }
        if (inventoryHasItem || player.npe.creativeCursorSlot == item) {
            player.npe.creativeCursorSlot = null
            return
        }

        player.npe.creativeCursorSlot = null
        wrapper.itemStack = ItemStack.builder()
            .type(item.type)
            .amount(item.amount)
            .build()

        // Add allowed components through
        val rules = RuleSet.Companion.rules(RuleSet.PacketItem)
            .filterIsInstance(GenericItemRule::class.java)

        val failed = rules.map { rule ->
            copyComponent(item, wrapper.itemStack, rule) to rule
        }.filter { !it.first }.map { it.second }

        // Check if it has changed in any way
        if (item != wrapper.itemStack) {
            val diff = PacketInspection.diff(item, wrapper.itemStack)

            val extra = Component.text("Click to copy Item Data", NamedTextColor.YELLOW)
            flag(
                event,
                Resolution.CANCEL,
                extra = extra,
                clickEvent = ClickEvent.copyToClipboard(item.toString())
            ) {
                "itemSize" to ByteBufHelper.readableBytes(buffer)
                "item" to item.type.name
                if (failed.isNotEmpty()) {
                    "violations" to "\n" + failed.joinToString("\n") { " - ${it.message}" }
                }
            }

            if (!player.npe.bypassing()) {
                // Rewrite the packet on the server
                wrapper.write()
                event.markForReEncode(true)

                // Notify the client
                if (config("notify_client_on_cancel", true)) {
                    val packet = WrapperPlayServerSetSlot(
                        0,
                        0,
                        wrapper.slot,
                        wrapper.itemStack
                    )

                    event.user.sendPacket(packet)
                }
            }
        }
    }

    /**
     * Converts a raw slot to the spigot slot
     * @param rawSlot The raw slot from the packet
     * @return The spigot slot
     */
    private fun toSpigotSlot(rawSlot: Int): Int {
        return when {
            rawSlot == -1 -> -1
            rawSlot == 5 -> 39
            rawSlot == 6 -> 38
            rawSlot == 7 -> 37
            rawSlot == 8 -> 36
            rawSlot < 8 -> rawSlot + 31
            rawSlot >= 36 && rawSlot <= 44 -> rawSlot - 36
            rawSlot == 45 -> 40
            else -> rawSlot
        }
    }

}