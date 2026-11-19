package no.nav.sikkerhetstjenesten.felles.rest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus.OK
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import java.net.URI

class RestHeaderAddingRequestInterceptorTest : BehaviorSpec({

    Given("a REST header adding request interceptor") {
        Then("configured headers are added and the request is passed through") {
            val request = MockClientHttpRequest(HttpMethod.GET, URI.create("/test"))
            val body = byteArrayOf(1, 2)
            val response = MockClientHttpResponse(ByteArray(0), OK)
            var executedRequest: HttpRequest? = null
            var executedBody: ByteArray? = null
            val execution = ClientHttpRequestExecution { requestToExecute, bodyToExecute ->
                executedRequest = requestToExecute
                executedBody = bodyToExecute
                response
            }

            val result = RestHeaderAddingRequestInterceptor("X-Test" to "value", "X-Other" to "another")
                .intercept(request, body, execution)

            request.headers["X-Test"] shouldBe listOf("value")
            request.headers["X-Other"] shouldBe listOf("another")
            executedRequest shouldBeSameInstanceAs request
            executedBody shouldBeSameInstanceAs body
            result shouldBeSameInstanceAs response
        }

        Then("headers are unchanged when none are configured") {
            val request = MockClientHttpRequest(HttpMethod.GET, URI.create("/test"))
            val execution = ClientHttpRequestExecution { _, _ -> MockClientHttpResponse(ByteArray(0), OK) }

            RestHeaderAddingRequestInterceptor().intercept(request, ByteArray(0), execution)

            request.headers.isEmpty shouldBe true
        }
    }
})
