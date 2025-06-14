package me.santio.npe.ruleset.item

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.nbt.NBT
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.github.retrooper.packetevents.protocol.nbt.NBTString
import com.google.auto.service.AutoService
import me.santio.npe.base.Processor
import me.santio.npe.ruleset.Rule
import java.util.function.Predicate

/**
 * Rule for item entity data.
 *
 * We normally want to completely drop this, however for some items we want to limit items
 * very heavily but allow certain things.
 *
 * @author santio
  */
@AutoService(Rule::class)
class ItemEntityDataRule: GenericItemRule<NBTCompound>(
    clazz = NBTCompound::class,
    componentType = ComponentTypes.ENTITY_DATA,
    config = "entity-data",
    message = "Invalid Item Entity Data",
) {

    private inline fun <reified T: NBT> NBTCompound.parse(name: String, predicate: Predicate<T>): T? {
        val data = this.tags[name] as? T ?: return null
        if (!predicate.test(data)) return null
        return data
    }

    override fun check(processor: Processor, itemStack: ItemStack, value: NBTCompound): Boolean {
        // Allow paintings to have specific variants allowed
        if (itemStack.type == ItemTypes.PAINTING) {
            if (!config(processor, "allow_paintings", true)) return false
            if (value.tagNames.filterNot { it in allowedPaintingKeys }.isNotEmpty()) return false

            value.parse<NBTString>("variant") { it.value.length <= 48 } ?: return false
            value.parse<NBTString>("id") { it.value == EntityTypes.PAINTING.name.toString() } ?: return false

            return true
        }

        // No other items may have entity data
        return false
    }

    private companion object {
        val allowedPaintingKeys = listOf("variant", "id")
    }

}