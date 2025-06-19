package me.santio.npe.listener

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.google.auto.service.AutoService
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.santio.npe.base.Resolution
import me.santio.npe.config.config
import me.santio.npe.data.user.AlertData
import me.santio.npe.data.user.npe
import me.santio.npe.listener.loader.annotations.Toggle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.*
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.jvm.optionals.getOrNull

@Suppress("unused")
@AutoService(Listener::class)
@Toggle("modules.massive-containers.enabled", default = true)
class RecursiveContainerListener: Listener {

    private val allowedTransactionTypes = setOf("PICKUP", "DROP", "CLONE", "COLLECT")

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onContainerInContainer(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.action == InventoryAction.NOTHING) return

        val playerInventory = player.inventory
        val openInventory = event.inventory

        var item = event.cursor.takeIf { !it.isEmpty } ?: event.currentItem ?: return
        if (allowedTransactionTypes.any { event.action.name.startsWith("${it}_") }) return

        val inventory = when (event.action) {
            InventoryAction.MOVE_TO_OTHER_INVENTORY -> {
                if (event.clickedInventory == playerInventory) openInventory else playerInventory
            }
            InventoryAction.HOTBAR_SWAP -> {
                item = playerInventory.getItem(event.hotbarButton) ?: return
                event.clickedInventory
            }
            else -> event.clickedInventory
        }

        val targetInventory = inventory?.takeIf {
            it.type != InventoryType.PLAYER && it.type != InventoryType.CRAFTING
        } ?: return

        val cancel = handleContainerMovement(player, targetInventory, item)
        if (cancel) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory.type == InventoryType.PLAYER || event.inventory.type == InventoryType.CRAFTING) return

        val player = event.player as? Player ?: return
        val rawItems = event.inventory.contents
            .filterNotNull()
            .associateWith {
                SpigotConversionUtil.fromBukkitItemStack(it)
            }

        val illegalItems = rawItems
            .filter { it.value.getComponent(ComponentTypes.CONTAINER).getOrNull()?.items?.isNotEmpty() ?: false }

        if (illegalItems.isEmpty()) return
        val resolution = config("modules.massive-containers.resolution", Resolution.CANCEL)

        if (resolution.shouldCancel) {
            event.inventory.contents.filterNotNull().forEach {
                if (rawItems.keys.contains(it)) event.inventory.remove(it)
            }
        }

        val alert = AlertData(
            player,
            "Illegal Chest Content",
            disconnect = resolution.shouldKick
        ) {
            "inventoryType" to event.inventory.type.toString().lowercase()
            "inventoryLocation" to (event.inventory.location?.toVector() ?: "null")
        }

        player.npe.flag(alert)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onHopperEnter(event: InventoryPickupItemEvent) {
        val itemStack = event.item.itemStack

        val cancel = handleContainerMovement(null, event.inventory, itemStack)
        if (cancel) event.isCancelled = true
    }

    private fun handleContainerMovement(player: Player?, inventory: Inventory, item: ItemStack): Boolean {
        val raw = SpigotConversionUtil.fromBukkitItemStack(item) ?: return false

        val container = raw.getComponent(ComponentTypes.CONTAINER)?.getOrNull() ?: return false
        if (container.items.isEmpty()) return false

        val resolution = config("modules.massive-containers.resolution", Resolution.CANCEL)

        if (player != null) {
            val alert = AlertData(
                player,
                "Illegal Chest Content",
                disconnect = resolution.shouldKick
            ) {
                "inventoryType" to inventory.type.toString().lowercase()
                "inventoryLocation" to (inventory.location?.toVector() ?: "null")
                "item" to raw.type.name.toString().lowercase()
                "itemContents" to container.items.size
            }

            player.npe.flag(alert)
        }

        return resolution.shouldCancel
    }

}