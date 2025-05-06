package no.iktdev.streamit.shared

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresAuthentication(val mode: Mode = Mode.Soft)

enum class Mode {
    Strict,
    Soft,
    None
}