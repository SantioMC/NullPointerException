package me.santio.npe.ruleset.item

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemPotionContents
import com.google.auto.service.AutoService
import me.santio.npe.ruleset.Rule

/**
 * Rule for potion contents.
 *
 * We don't want to allow any custom defined potion effects
 *
 * @author santio
  */
@AutoService(Rule::class)
class ItemPotionRule: GenericItemRule<ItemPotionContents>(
    clazz = ItemPotionContents::class,
    componentType = ComponentTypes.POTION_CONTENTS,
    message = "Invalid Potion Data",
) {

    override fun check(value: ItemPotionContents): Boolean {

        return false
    }

}