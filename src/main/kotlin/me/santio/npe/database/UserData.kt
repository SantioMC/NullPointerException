package me.santio.npe.database

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.bindings.Bindings
import gg.ingot.iron.strategies.NamingStrategy
import java.util.*

/**
 * Persistent data stored for a player
 */
@Model(table = "user_settings", naming = NamingStrategy.SNAKE_CASE)
data class UserData(
    @Column(name = "id")
    val uuid: UUID,
    var alerts: Boolean = false,
    var ignoreBypass: Boolean = false
): Bindings
