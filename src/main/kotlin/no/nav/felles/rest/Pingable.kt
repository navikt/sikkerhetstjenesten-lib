package no.nav.felles.rest

import java.net.URI

interface Pingable {
    fun ping(): Any?
    val pingEndpoint: URI
    val name: String
}
