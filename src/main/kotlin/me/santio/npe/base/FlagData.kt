package me.santio.npe.base

class FlagData(
    private val mapping: MutableMap<String, Any> = mutableMapOf()
) {

    infix fun String.to(value: Any) {
        mapping[this] = value
    }

    fun map(): Map<String, Any> {
        return mapping
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("(")

        for ((key, value) in mapping) {
            builder.append(key)
            builder.append(": ")
            builder.append(value.toString())
            builder.append(", ")
        }

        builder.delete(builder.length - 3, builder.length - 1)
        builder.append(")")

        return builder.toString()
    }
}