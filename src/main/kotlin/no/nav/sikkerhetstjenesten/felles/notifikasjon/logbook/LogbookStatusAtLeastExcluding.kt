package no.nav.sikkerhetstjenesten.felles.notifikasjon.logbook

import org.springframework.http.HttpStatus
import org.zalando.logbook.Correlation
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.HttpResponse
import org.zalando.logbook.Precorrelation
import org.zalando.logbook.Sink
import org.zalando.logbook.Strategy
import org.zalando.logbook.core.StatusAtLeastStrategy


class LogbookStatusAtLeastExcluding(atLeast: HttpStatus, private vararg val excludedStatus: HttpStatus) : Strategy {
    private val delegate = StatusAtLeastStrategy(atLeast.value())

    override fun write(precorrelation: Precorrelation, request: HttpRequest, sink: Sink) {
        if (!request.shouldIgnoreGraphQlIntrospectionQuery()) {
            delegate.write(precorrelation, request, sink)
        }
    }

    override fun write(correlation: Correlation, req: HttpRequest, res: HttpResponse, sink: Sink) {
        if (!req.shouldIgnoreGraphQlIntrospectionQuery() && res.status !in (excludedStatus.map { it.value() })) {
            delegate.write(correlation, req, res, sink)
        }
    }
}