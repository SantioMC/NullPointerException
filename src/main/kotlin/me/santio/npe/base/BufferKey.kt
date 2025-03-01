package me.santio.npe.base

data class BufferKey(
    val processor: Processor,
    val key: String
) {
    override fun toString(): String {
        return "$processor:$key"
    }
}
