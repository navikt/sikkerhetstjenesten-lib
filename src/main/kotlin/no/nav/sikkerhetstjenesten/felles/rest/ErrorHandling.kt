package no.nav.sikkerhetstjenesten.felles.rest


import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.REQUEST_TIMEOUT
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponseException
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import java.net.URI

class DefaultRestErrorHandler : ErrorHandler {
    private val log = getLogger(javaClass)

    override fun handle(req: HttpRequest, res: ClientHttpResponse) {
        val status = res.statusCode
        val uri = req.uri
        val ident = req.headers.getFirst(IDENTIFIKATOR)

        val e = when (status) {
            NOT_FOUND -> NotFoundRestException(uri, ident)
            REQUEST_TIMEOUT, TOO_MANY_REQUESTS ->
                RecoverableRestException(status, uri, res.statusText)
            else ->
                if (status.is4xxClientError) {
                    IrrecoverableRestException(status, uri, res.statusText)
                } else {
                    RecoverableRestException(status, uri, res.statusText)
                }
        }
        val exceptionName = e::class.simpleName

        if (e is NotFoundRestException) {
            log.info("$exceptionName etter ${status.value()} fra $uri")
        } else {
            log.warn("$exceptionName etter ${status.value()} fra $uri")
        }

        throw e
    }

    companion object {
        const val IDENTIFIKATOR = "X-Identifikator"
    }
}

