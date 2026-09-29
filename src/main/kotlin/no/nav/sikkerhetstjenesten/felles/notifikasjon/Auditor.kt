package no.nav.sikkerhetstjenesten.felles.notifikasjon

@FunctionalInterface
interface Auditor {
    fun info(message: String, t: Throwable? = null)
}
