package me.santio.npe.tasks

import me.santio.npe.NPE
import me.santio.npe.data.user.NPEUser

object AlertBroadcastTask: Runnable {

    override fun run() {
        for (user in NPEUser.users.values) {
            for (alert in user.debounce.values) {
                val component = alert.component() ?: continue
                NPE.broadcast(component)
            }

            user.debounce.clear()
        }
    }

}