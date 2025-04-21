package me.santio.npe.data

object DataConverter {

    @Suppress("UNCHECKED_CAST")
    fun <T> cast(raw: String, into: Class<T>): T? {
        return when (into) {
            String::class.java -> raw
            Int::class.java -> raw.toInt()
            Long::class.java -> raw.toLong()
            Double::class.java -> raw.toDouble()
            Boolean::class.java -> raw.toBoolean()
            Float::class.java -> raw.toFloat()
            Byte::class.java -> raw.toByte()
            Short::class.java -> raw.toShort()
            Char::class.java -> raw[0]
            else -> into.cast(raw)
        }?.let { it as T }
    }

    fun isSupported(into: Class<*>): Boolean {
        return into.isPrimitive || into == String::class.java
    }

    /**
     * Split the message in spaces, except allowing spaces in quoted strings
     * @param message the message
     * @return the split message
     */
    fun split(message: String): List<String> {
        var inQuotes = false

        var buffer = ""
        val parts = mutableListOf<String>()
        val split = message.split(' ')

        fun pushBuffer(content: String) {
            if (buffer.isNotEmpty()) buffer += " "
            buffer += content
        }

        for (part in split) {
            if (part.startsWith('"') && !inQuotes) {
                inQuotes = true
                if (part.length > 1) {
                    pushBuffer(part.substring(1))
                }
            } else if (part.endsWith('"') && inQuotes) {
                inQuotes = false
                if (part.length > 1) {
                    pushBuffer(part.substring(0, part.length - 1))
                }
                parts.add(buffer)
                buffer = ""
            } else if (inQuotes) {
                pushBuffer(part)
            } else {
                parts.add(part)
            }
        }

        return parts
    }

}