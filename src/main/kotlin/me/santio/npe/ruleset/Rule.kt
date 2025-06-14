package me.santio.npe.ruleset

import com.github.retrooper.packetevents.protocol.item.ItemStack
import me.santio.npe.base.Processor
import kotlin.reflect.KClass

/**
 * Represents a rule that must be enforced, this is mostly just a generic predicate with
 * the ability to attempt to correct the value if it fails.
 * @param T The type of the value to check
 * @author santio
 */
abstract class Rule<T: Any>(
    open val clazz: KClass<T>,
    open val config: String,
    open val ruleset: RuleSet,
    open val message: String
) {

    /**
     * Checks the value, this is the actual predicate that is used to check if the rule passes
     * @param processor The processor that is using the rule
     * @param itemStack The item stack for reference, do not modify this item
     * @param value The value to check
     * @return True if the rule passes, false otherwise
     */
    abstract fun check(processor: Processor, itemStack: ItemStack, value: T): Boolean

    /**
     * Attempts to produce a corrected value, if the value is null then the value should be completely
     * stripped if possible or an error should be thrown, whatever fits the implementation. The default
     * behaviour is to return null.
     * @param processor The processor that is using the rule
     * @param itemStack The item stack for reference, do not modify this item
     * @param value The value to correct
     * @return The corrected value, or null if the value is null
     */
    open fun correct(processor: Processor, itemStack: ItemStack, value: T): T? { return null }

    //region Config Helpers
    fun config(processor: Processor, key: String): String? {
        return processor.config("rules.$config.$key")
    }

    fun config(processor: Processor, key: String, default: String): String {
        return processor.config("rules.$config.$key", default)
    }

    inline fun <reified T: Any> config(processor: Processor, key: String, default: T): T {
        return processor.config<T>("rules.$config.$key", default)
    }
    //endregion

}