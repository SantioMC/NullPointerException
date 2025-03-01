package me.santio.npe.processor

import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.google.auto.service.AutoService
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import me.santio.npe.base.Processor
import me.santio.npe.data.npe
import me.santio.npe.ruleset.RuleSet
import me.santio.npe.ruleset.item.GenericItemRule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

@AutoService(Processor::class)
class ItemProcessor: Processor("Illegal Items", clone = false, priority = PacketListenerPriority.LOW) {

    override fun filter(): List<PacketTypeCommon> {
        return listOf(
            PacketType.Play.Client.CREATIVE_INVENTORY_ACTION
        )
    }

    private fun <T: Any>copyComponent(original: ItemStack, new: ItemStack, rule: GenericItemRule<T>) {
        val originalComponent = original.getComponent(rule.componentType)
        if (originalComponent.isPresent) {
            // only copy if it matches rule
            if (!rule.check(originalComponent.get())) {
                // See if we can correct it
                val corrected = rule.correct(originalComponent.get()) ?: return
                new.setComponent(rule.componentType, corrected)
            } else {
                // All good, just copy it
                new.setComponent(rule.componentType, originalComponent.get())
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getPacket(event: PacketReceiveEvent) {
        val buffer = event.fullBufferClone
        val wrapper = WrapperPlayClientCreativeInventoryAction(event)

        val item = wrapper.itemStack
        val player = event.getPlayer<Player>()

        if (player.npe.bypassing()) return

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
        val rules = RuleSet.rules(RuleSet.PacketItem)
            .filterIsInstance(GenericItemRule::class.java)

        rules.forEach { rule ->
            copyComponent(item, wrapper.itemStack, rule)
        }

        // Check if it has changed in any way
        if (item != wrapper.itemStack) {
            wrapper.write()
            event.markForReEncode(true)

            val extra = Component.text("Click to copy Item Data", NamedTextColor.YELLOW)
            flag(
                event,
                disconnect = false,
                cancelEvent = false,
                extra = extra,
                clickEvent = ClickEvent.copyToClipboard(item.toString())
            ) {
                "itemSize" to ByteBufHelper.readableBytes(buffer)
                "item" to item.type.name
            }

            val packet = WrapperPlayServerSetSlot(
                0,
                0,
                wrapper.slot,
                wrapper.itemStack
            )

            event.user.sendPacket(packet)
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
