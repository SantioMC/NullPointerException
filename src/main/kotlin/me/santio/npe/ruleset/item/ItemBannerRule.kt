package me.santio.npe.ruleset.item

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.component.builtin.item.BannerLayers
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.google.auto.service.AutoService
import me.santio.npe.base.Processor
import me.santio.npe.ruleset.Rule

/**
 * Rule for banner layers.
 *
 * We want to limit the banner layers to 12 layers.
 *
 * @author santio
  */
@AutoService(Rule::class)
class ItemBannerRule: GenericItemRule<BannerLayers>(
    clazz = BannerLayers::class,
    componentType = ComponentTypes.BANNER_PATTERNS,
    config = "banner-data",
    message = "Invalid Banner Data",
) {
    override fun check(processor: Processor, itemStack: ItemStack, value: BannerLayers): Boolean {
        return value.layers.size <= config(processor, "max_layers", 16)
    }

    override fun correct(processor: Processor, itemStack: ItemStack, value: BannerLayers): BannerLayers {
        return BannerLayers(value.layers.take(config(processor, "max_layers", 16)))
    }
}