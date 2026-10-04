package no.nav.sikkerhetstjenesten.felles.rest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import no.nav.sikkerhetstjenesten.felles.rest.DefaultRestErrorHandler.Companion.IDENTIFIKATOR
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.REQUEST_TIMEOUT
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import java.net.URI

class DefaultRestErrorHandlerTest : BehaviorSpec({

    val handler = DefaultRestErrorHandler()
    val uri = URI.create("http://test-service/api/resource")

    fun req(ident: String? = null) = MockClientHttpRequest(GET, uri).apply {
        ident?.let { headers.set(IDENTIFIKATOR, it) }
    }

    fun res(status: HttpStatus) = withStatus(status).createResponse(null)

    Given("handle - 404 Not Found") {
        When("request uten identifikator") {
            Then("kastes NotFoundRestException med riktig URI og null identifikator") {
                shouldThrow<NotFoundRestException> { handler.handle(req(), res(NOT_FOUND)) }.apply {
                    assertSoftly {
                        uri shouldBe uri
                        identifikator shouldBe null
                    }
                }
            }
        }
        When("request med identifikator-header") {
            val id = "12345678901"
            Then("kastes NotFoundRestException med identifikator fra header") {
                shouldThrow<NotFoundRestException> {
                    handler.handle(req(id), res(NOT_FOUND))
                }.identifikator shouldBe id
            }
        }
    }

    Given("handle - 4xx klientfeil (ikke 404)") {
        When("400 Bad Request") {
            Then("kastes IrrecoverableRestException, ikke NotFoundRestException") {
                shouldThrow<IrrecoverableRestException> { handler.handle(req(), res(BAD_REQUEST)) }.apply {
                    shouldBeInstanceOf<IrrecoverableRestException>()
                    shouldNotBeInstanceOf<NotFoundRestException>()
                }
            }
        }
        When("403 Forbidden") {
            Then("kastes IrrecoverableRestException") {
                shouldThrow<IrrecoverableRestException> {
                    handler.handle(req(), res(FORBIDDEN))
                }
            }
        }
        When("429 Too Many Requests") {
            Then("kastes RecoverableRestException") {
                shouldThrow<RecoverableRestException> {
                    handler.handle(req(), res(TOO_MANY_REQUESTS))
                }
            }
        }
        When("408 Request Timeout") {
            Then("kastes RecoverableRestException") {
                shouldThrow<RecoverableRestException> {
                    handler.handle(req(), res(REQUEST_TIMEOUT))
                }
            }
        }
    }

    Given("handle - 5xx serverfeil") {
        When("500 Internal Server Error") {
            Then("kastes RecoverableRestException, ikke IrrecoverableRestException") {
                shouldThrow<RecoverableRestException> {
                    handler.handle(req(), res(INTERNAL_SERVER_ERROR))
                }.shouldNotBeInstanceOf<IrrecoverableRestException>()
            }
        }
        When("503 Service Unavailable") {
            Then("kastes RecoverableRestException") {
                shouldThrow<RecoverableRestException> {
                    handler.handle(req(), res(SERVICE_UNAVAILABLE))
                }
            }
        }
    }
})
