package no.nav.sikkerhetstjenesten.felles.rest


import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.web.ErrorResponseException
import java.net.URI

open class IrrecoverableRestException(
    val status: HttpStatusCode, uri: URI, msg: String = (status as HttpStatus).reasonPhrase,
    cause: Throwable? = null) : ErrorResponseException(status, problemDetail(status, msg, uri), cause)

class NotFoundRestException(val uri: URI,
                            val identifikator: String? = null,
                            msg: String = "Ikke funnet",
                            cause: Throwable? = null) :
    IrrecoverableRestException(NOT_FOUND, uri, msg, cause)

class ConflictRestException(val uri: URI, msg: String = "Konflikt", cause: Throwable? = null) :
    IrrecoverableRestException(CONFLICT, uri, msg, cause)

open class RecoverableRestException(status: HttpStatusCode,
                                    uri: URI,
                                    msg: String = (status as HttpStatus).reasonPhrase,
                                    cause: Throwable? = null) :
    ErrorResponseException(status, problemDetail(status, msg, uri), cause)

private fun problemDetail(status: HttpStatusCode, msg: String, uri: URI) =
    forStatusAndDetail(status, msg).apply {
        title = "${status.value()}"
        properties = mapOf("uri" to "$uri")
    }