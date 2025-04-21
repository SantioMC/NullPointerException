package me.santio.npe.config

/*
 * Handles configuration for NPE, this allows for complete configuration for
 * every part of the plugin.
 */
import me.santio.npe.NPE

fun config(key: String): String? {
    return NPE.instance.config.getString(key)
}

fun config(key: String, default: String): String {
    return NPE.instance.config.getString(key, default)!!
}

@Suppress("UNCHECKED_CAST")
fun <T: Any> parseFromConfig(clazz: Class<T>, value: Any?): T? {
    if (value == null) return null
    if (clazz == String::class.java) {
        return value as? T
    }

    if (clazz.isEnum) {
        val enumClazz = clazz as Class<out Enum<*>>
        return java.lang.Enum.valueOf(enumClazz, (value as String).uppercase()) as? T
    }

    return value as? T
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> config(key: String, default: T): T {
    if (T::class.java.isAssignableFrom(List::class.java)) {
        val listClazz = T::class.java as Class<out List<*>>
        val superClass = listClazz.genericSuperclass as? Class<Any>
            ?: String::class.java

        return NPE.instance.config.getStringList(key)
            .mapNotNull { parseFromConfig(superClass, it) }
            .toList() as? T
            ?: run {
                NPE.logger.warn("Failed to find valid super class for $key, defaulting...")
                return default
            }
    }

    return parseFromConfig(T::class.java, NPE.instance.config.get(key)) ?: default
}
