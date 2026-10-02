package no.nav.sikkerhetstjenesten.felles.rest


import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponseException
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import java.net.URI

@Component
@Primary
class DefaultRestErrorHandler : ErrorHandler {
    private val log = getLogger(javaClass)

    override fun handle(req: HttpRequest, res: ClientHttpResponse) {
        when {
            res.statusCode == NOT_FOUND -> {
                log.info("Not found fra ${req.uri}")
                throw NotFoundRestException(req.uri, res.statusText)
            }
            res.statusCode.is4xxClientError -> {
                log.warn("Irrecoverable exception etter ${res.statusCode.value()} fra ${req.uri}")
                throw IrrecoverableRestException(res.statusCode, req.uri, res.statusText)
            }
            else -> {
                log.warn("Recoverable exception etter ${res.statusCode.value()} fra ${req.uri}")
                throw RecoverableRestException(res.statusCode, req.uri, res.statusText)
            }
        }
    }
}

