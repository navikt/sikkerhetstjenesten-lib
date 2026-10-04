package no.nav.sikkerhetstjenesten.felles.security

import io.opentelemetry.api.trace.Span
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatusAndDetail
import java.net.URI

fun securityProblemDetail(status: HttpStatus, detail: String, typeURI: URI): ProblemDetail =
    forStatusAndDetail(status, detail).apply {
        type = typeURI
        title = "${status.value()}"
        properties = mapOf("traceId" to Span.current().spanContext.traceId)
    }
