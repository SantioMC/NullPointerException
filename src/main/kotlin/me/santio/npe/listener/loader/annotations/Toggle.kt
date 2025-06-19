package me.santio.npe.listener.loader.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Toggle(
    val key: String,
    val default: Boolean = false
)
