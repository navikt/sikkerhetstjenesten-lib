package no.nav.sikkerhetstjenesten.felles.rest


import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.sikkerhetstjenesten.felles.security.AuthContext
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class ConsumerAwareHandlerInterceptor(private val token: AuthContext, private val registry: MeterRegistry) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        MDC.put(CONSUMER_ID, token.systemAndNs)
        token.ansattId?.verdi?.let { MDC.put(USER_ID, it) }
        registry.counter(METRIC, Tags.of("remote_system",token.systemNavn)).increment()
        return true
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        MDC.remove(CONSUMER_ID)
        MDC.remove(USER_ID)
    }

    companion object  {
        private const val METRIC = "http_requests_by_remote_system"
        const val CONSUMER_ID = "consumerId"
        const val USER_ID = "userId"
    }
}
