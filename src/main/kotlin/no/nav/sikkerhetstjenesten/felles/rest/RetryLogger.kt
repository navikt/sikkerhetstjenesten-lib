package no.nav.sikkerhetstjenesten.felles.rest

import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.event.EventListener
import org.springframework.core.retry.RetryException
import org.springframework.resilience.retry.MethodRetryEvent
import org.springframework.stereotype.Component

@Component
class RetryLogger {
    private val log = getLogger(javaClass)

    @EventListener(MethodRetryEvent::class)
    fun onEvent(event: MethodRetryEvent) {
        if (event.isRetryAborted) {
            (event.failure as RetryException).let { state ->
                log.warn(
                    "Metode '${event.method.name}' feilet etter ${state.retryCount} retries",
                    state.lastException
                )
            }
        }
        else  {
            log.info("Retry metode '${event.method.name}', ${event.failure}")
        }
    }
}