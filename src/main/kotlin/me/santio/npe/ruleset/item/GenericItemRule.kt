package me.santio.npe.ruleset.item

import com.github.retrooper.packetevents.protocol.component.ComponentType
import me.santio.npe.ruleset.Rule
import me.santio.npe.ruleset.RuleSet
import kotlin.reflect.KClass

abstract class GenericItemRule<C: Any>(
    override val clazz: KClass<C>,
    val componentType: ComponentType<C>,
    override val message: String
): Rule<C>(
    clazz = clazz,
    ruleset = RuleSet.PacketItem,
    message = message
)
