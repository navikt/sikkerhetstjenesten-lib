package no.nav.sikkerhetstjenesten.felles.rest

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class RestHeaderAddingRequestInterceptor(vararg headers: Pair<String, String>) : ClientHttpRequestInterceptor {
    private val headers = headers.toList()

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        headers.forEach { (key, value) -> request.headers.add(key, value) }
        return execution.execute(request, body)
    }
}
