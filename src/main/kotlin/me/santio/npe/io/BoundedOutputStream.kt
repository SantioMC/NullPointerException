package me.santio.npe.io

import java.io.OutputStream

/**
 * Creates an output stream that has a fixed capacity and will begin to write
 * over data once the capacity is reached.
 *
 * This is useful for writing to a file, as the file will not grow indefinitely
 * if the stream is not bounded.
 *
 * @author santio
 */
@Suppress("unused")
class BoundedOutputStream(
    private val capacity: Int
): OutputStream() {

    private val buf = ByteArray(capacity)
    private var position = 0
    private var overflowed = false

    override fun write(b: Int) {
        buf[position] = b.toByte()
        position = (position + 1) % capacity
        if (position == 0) overflowed = true
    }

    /**
     * @return The internal buffer as a byte array
     */
    fun toByteArray(): ByteArray {
        if (position == 0) return ByteArray(0)
        if (position == capacity) return buf.copyOfRange(0, capacity)
        if (!overflowed) return buf.copyOfRange(0, position)

        val first = buf.copyOfRange(position, buf.size)
        val second = buf.copyOfRange(0, position)
        return first + second
    }
}