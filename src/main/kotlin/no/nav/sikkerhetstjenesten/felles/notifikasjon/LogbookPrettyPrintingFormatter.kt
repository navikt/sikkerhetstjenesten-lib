package no.nav.sikkerhetstjenesten.felles.notifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import org.zalando.logbook.Correlation
import org.zalando.logbook.HttpLogFormatter
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.HttpResponse
import org.zalando.logbook.Precorrelation
import org.zalando.logbook.json.JsonHttpLogFormatter

class LogbookPrettyPrintingFormatter(private val mapper: ObjectMapper) : HttpLogFormatter {
    private val delegate = JsonHttpLogFormatter(mapper)

    override fun format(precorrelation: Precorrelation, request: HttpRequest) =
        prettyPrint(delegate.format(precorrelation, request))

    override fun format(correlation: Correlation, response: HttpResponse) =
        prettyPrint(delegate.format(correlation, response))

    private fun prettyPrint(raw: String) =
        runCatching {
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(raw))
        }.getOrDefault(raw)
}
