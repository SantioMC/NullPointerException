package me.santio.npe.ruleset.item

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.component.builtin.item.FireworkExplosion
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemFireworks
import com.google.auto.service.AutoService
import me.santio.npe.base.Processor
import me.santio.npe.ruleset.Rule

/**
 * Rule for firework rockets.
 *
 * Prevents spawning in firework rockets with values that are not possible
 * in regular creative mode
 *
 * @author santio
  */
@AutoService(Rule::class)
class FireworkRule: GenericItemRule<ItemFireworks>(
    clazz = ItemFireworks::class,
    componentType = ComponentTypes.FIREWORKS,
    config = "fireworks",
    message = "Invalid Firework Data"
) {

    override fun check(
        processor: Processor,
        value: ItemFireworks
    ): Boolean {
        return value.explosions.isEmpty() && value.flightDuration <= 3 && value.flightDuration >= 0
    }

    override fun correct(
        processor: Processor,
        value: ItemFireworks
    ): ItemFireworks {
        return ItemFireworks(
            value.flightDuration.coerceAtMost(3),
            emptyList<FireworkExplosion>()
        )
    }
}