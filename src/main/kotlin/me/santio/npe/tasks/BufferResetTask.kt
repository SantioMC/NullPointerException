package me.santio.npe.tasks

import me.santio.npe.data.NPEUser

object BufferResetTask: Runnable {

    override fun run() {
        NPEUser.users.forEach { it.value.buffer.clear() }
    }

}