package me.santio.npe.ruleset.item

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.google.auto.service.AutoService
import me.santio.npe.base.Processor
import me.santio.npe.ruleset.Rule

/**
 * Rule for player head profile components.
 *
 * The profile data should be valid, have no missing data, no additional data, and resolve to a valid URL.
 *
 * @author santio
  */
@AutoService(Rule::class)
class ItemOminousBottleRule: GenericItemRule<Int>(
    clazz = Int::class,
    componentType = ComponentTypes.OMINOUS_BOTTLE_AMPLIFIER,
    config = "ominous-bottle-data",
    message = "Invalid Ominous Bottle Data",
) {

    override fun check(processor: Processor, itemStack: ItemStack, value: Int): Boolean {
        return value >= 0 && value <= 4
    }

    override fun correct(processor: Processor, itemStack: ItemStack, value: Int): Int? {
        return value.coerceIn(0, 4)
    }
}