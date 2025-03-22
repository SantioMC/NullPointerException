package me.santio.npe.base

enum class Resolution(
    val shouldKick: Boolean,
    val shouldCancel: Boolean
) {
    KICK(true, true),
    CANCEL(false, true),
    IGNORE(false, false),
    ;
}