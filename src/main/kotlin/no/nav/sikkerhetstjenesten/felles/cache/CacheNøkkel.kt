package no.nav.sikkerhetstjenesten.felles.cache

import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions.maskFnr

data class CacheNøkkel (val nøkkel: String) {
    private val elementer = nøkkel.split("::", ":")
    val cacheName = elementer.first()
    val metode = if (elementer.size > 2) elementer[1] else null
    val id = elementer.last()

    override fun toString() =
         "CacheNøkkel(cacheName=$cacheName, metode=$metode, id=${id.maskFnr()})"
}