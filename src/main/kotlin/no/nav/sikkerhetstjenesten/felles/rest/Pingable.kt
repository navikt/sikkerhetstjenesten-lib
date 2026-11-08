package no.nav.sikkerhetstjenesten.felles.rest

import java.net.URI

interface Pingable {

    fun ping(): Any?
    val pingEndpoint: URI
    val name: String
}

fun pingable(cfg: RestConfig, pingAction: () -> Any?): Pingable =
    object : Pingable {
        override val name = cfg.name
        override val pingEndpoint = cfg.pingEndpoint
        override fun ping() = pingAction()
    }