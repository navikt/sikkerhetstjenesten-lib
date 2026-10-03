package no.nav.sikkerhetstjenesten.felles.rest

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