package me.santio.npe.ruleset.item

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.component.builtin.item.BannerLayers
import com.google.auto.service.AutoService
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
    message = "Invalid Banner Data",
) {
    override fun check(value: BannerLayers): Boolean {
        return value.layers.size <= 16
    }

    override fun correct(value: BannerLayers): BannerLayers {
        return BannerLayers(value.layers.take(16))
    }
}