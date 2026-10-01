
package no.nav.sikkerhetstjenesten.felles.domain

import no.nav.sikkerhetstjenesten.felles.domain.BrukerId.Companion.BRUKERID_LENGTH


object DomainExtensions {
    const val UTILGJENGELIG = "N/A"

    fun String.maskFnr() =
        when (length) {
            BRUKERID_LENGTH -> replaceRange(4, 11, "*******")
            13 -> replaceRange(6, 13, "*******")
            else -> this
        }

    fun requireDigits(verdi: String, len: Int) {
        require(verdi.all { it.isDigit() }) { "Ugyldig(e) tegn i $verdi, forventet $len siffer" }
        require(verdi.length == len) { "Ugyldig lengde ${verdi.length} for $verdi, forventet $len siffer" }
    }
}
