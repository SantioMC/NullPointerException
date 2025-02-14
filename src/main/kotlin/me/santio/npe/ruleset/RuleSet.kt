package me.santio.npe.ruleset

import me.santio.npe.NPE
import java.util.*
import kotlin.reflect.KClass

/**
 * Represents a set of rules that must be enforced for specific entities
 * @author santio
 */
enum class RuleSet {

    PacketItem, // ItemStack Components
    ;

    companion object {

        private val rules = mutableMapOf<RuleSet, MutableList<Rule<*>>>()

        internal fun loadRules() {
            val serviceLoader = ServiceLoader.load(
                Rule::class.java,
                this.javaClass.classLoader
            )

            serviceLoader.forEach { rule ->
                rules.getOrPut(rule.ruleset) { mutableListOf() }.add(rule)
            }

            NPE.logger.info("Loaded {} rules", rules.values.flatten().size)
        }

        fun rules(ruleset: RuleSet): List<Rule<*>> {
            return rules[ruleset] ?: emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        fun <T: Any>find(clazz: KClass<T>): List<Rule<T>> {
            return rules.values.flatten()
                .filter { it.clazz == clazz }
                .map { it as Rule<T> }
        }

        inline fun <reified T: Any>find(): List<Rule<T>> {
            return find(T::class)
        }
    }

}