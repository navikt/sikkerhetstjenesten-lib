package no.nav.sikkerhetstjenesten.felles.rest

import org.springframework.http.client.ClientHttpRequestInterceptor
import java.net.URI

object DownstreamURIContext {
    private val downstreamUri = ThreadLocal<String>()

    val currentUri get() = URI.create(downstreamUri.get())

    fun set(uri: String) {
        downstreamUri.set(uri)
    }

    fun clear() {
        downstreamUri.remove()
    }
}

fun headerAddingRequestInterceptor(vararg verdier: Pair<String, String>) =
    ClientHttpRequestInterceptor { request, body, next ->
        verdier.forEach { (key, value) -> request.headers.add(key, value) }
        next.execute(request, body)
    }