package me.santio.npe.ruleset

import kotlin.reflect.KClass

/**
 * Represents a rule that must be enforced, this is mostly just a generic predicate with
 * the ability to attempt to correct the value if it fails.
 * @param T The type of the value to check
 * @author santio
 */
abstract class Rule<T: Any>(
    open val clazz: KClass<T>,
    open val ruleset: RuleSet,
    open val message: String
) {

    /**
     * Checks the value, this is the actual predicate that is used to check if the rule passes
     * @param value The value to check
     * @return True if the rule passes, false otherwise
     */
    abstract fun check(value: T): Boolean

    /**
     * Attempts to produce a corrected value, if the value is null then the value should be completely
     * stripped if possible or an error should be thrown, whatever fits the implementation. The default
     * behaviour is to return null.
     * @param value The value to correct
     * @return The corrected value, or null if the value is null
     */
    open fun correct(value: T): T? { return null }

}