package me.santio.npe.data

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

/**
 * Persistent data stored for a player
 */
@Entity(name = "user_data")
@Table(name = "user_data")
class UserData(
    @Id
    @Column(name = "id")
    val uniqueId: UUID,
    var alerts: Boolean = false,
    var ignoreBypass: Boolean = false
)