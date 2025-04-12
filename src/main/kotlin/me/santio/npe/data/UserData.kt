package me.santio.npe.data

/**
 * Persistent data stored for a player
 */
data class UserData(
    var alerts: Boolean = false,
    var ignoreBypass: Boolean = false
) {

    fun hasData(): Boolean {
        return this != default
    }

    private companion object {
        private val default = UserData()
    }
}