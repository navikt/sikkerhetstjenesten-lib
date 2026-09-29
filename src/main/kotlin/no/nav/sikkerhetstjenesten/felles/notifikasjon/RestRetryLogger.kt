package no.nav.sikkerhetstjenesten.felles.notifikasjon

import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.event.EventListener
import org.springframework.core.retry.RetryException
import org.springframework.resilience.retry.MethodRetryEvent


class RestRetryLogger {
    private val log = getLogger(javaClass)

    @EventListener(MethodRetryEvent::class)
    fun onEvent(event: MethodRetryEvent) {
        val args = event.source.arguments.toSet()
        val metode = event.method.name
        when (val t = event.failure) {
            is RetryException -> if (t.exceptions.size > 1) {
                log.warn("Aborterer metode '$metode' etter ${t.exceptions.size} forsøk grunnet ${t.cause.javaClass.simpleName} $args", t)
            }
            else -> log.info("Feil i '$metode' grunnet ${t.javaClass.simpleName}", t)
        }
    }

}