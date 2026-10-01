package no.nav.sikkerhetstjenesten.felles.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus.OK
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import java.net.URI

class RestHeaderAddingRequestInterceptorTest {

    @Test
    fun `should add configured headers and pass request through`() {
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

        assertEquals(listOf("value"), request.headers["X-Test"])
        assertEquals(listOf("another"), request.headers["X-Other"])
        assertSame(request, executedRequest)
        assertSame(body, executedBody)
        assertSame(response, result)
    }

    @Test
    fun `should leave headers unchanged when none are configured`() {
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("/test"))
        val execution = ClientHttpRequestExecution { _, _ -> MockClientHttpResponse(ByteArray(0), OK) }

        RestHeaderAddingRequestInterceptor().intercept(request, ByteArray(0), execution)

        assertTrue(request.headers.isEmpty())
    }
}
