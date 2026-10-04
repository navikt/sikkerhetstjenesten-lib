package no.nav.sikkerhetstjenesten.felles.graphql

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.felles.rest.IrrecoverableRestException
import no.nav.sikkerhetstjenesten.felles.rest.RecoverableRestException
import org.springframework.graphql.ResponseError
import org.springframework.graphql.client.ClientGraphQlResponse
import org.springframework.graphql.client.FieldAccessException
import org.springframework.graphql.client.GraphQlTransportException
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNAUTHORIZED
import java.net.URI

class GraphQLErrorHandlerTest : BehaviorSpec({

    val handler = GraphQLErrorHandler()
    val uri = URI.create("http://test/graphql")

    fun fieldAccessException(vararg errors: ResponseError): FieldAccessException {
        val response = mockk<ClientGraphQlResponse> {
            every { this@mockk.errors } returns errors.toList()
        }
        return FieldAccessException(mockk(), response, mockk(relaxed = true))
    }

    fun responseError(code: String) = mockk<ResponseError> {
        every { extensions } returns mapOf("code" to code)
    }

    Given("handle - FieldAccessException") {
        When("kode er UNAUTHENTICATED") {
            Then("kastes IrrecoverableRestException med UNAUTHORIZED status") {
                shouldThrow<IrrecoverableRestException> {
                    handler.handle(uri, fieldAccessException(responseError("UNAUTHENTICATED")))
                }.statusCode shouldBe UNAUTHORIZED
            }
        }
        When("kode er FORBIDDEN") {
            Then("kastes IrrecoverableRestException med FORBIDDEN status") {
                shouldThrow<IrrecoverableRestException> {
                    handler.handle(uri, fieldAccessException(responseError("FORBIDDEN")))
                }.statusCode shouldBe FORBIDDEN
            }
        }
        When("ingen errors") {
            Then("kastes IrrecoverableRestException med INTERNAL_SERVER_ERROR status") {
                shouldThrow<IrrecoverableRestException> {
                    handler.handle(uri, fieldAccessException())
                }.statusCode shouldBe INTERNAL_SERVER_ERROR
            }
        }
    }

    Given("handle - GraphQlTransportException") {
        When("transport-feil oppstår") {
            Then("kastes RecoverableRestException med INTERNAL_SERVER_ERROR status") {
                shouldThrow<RecoverableRestException> {
                    handler.handle(uri, GraphQlTransportException("timeout", RuntimeException(), mockk()))
                }.statusCode shouldBe INTERNAL_SERVER_ERROR
            }
        }
    }

    Given("handle - annen exception") {
        When("ukjent exception kastes") {
            Then("kastes IrrecoverableRestException med INTERNAL_SERVER_ERROR status") {
                shouldThrow<IrrecoverableRestException> {
                    handler.handle(uri, RuntimeException("noe gikk galt"))
                }.statusCode shouldBe INTERNAL_SERVER_ERROR
            }
        }
    }
})
