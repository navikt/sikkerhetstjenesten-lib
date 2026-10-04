package no.nav.sikkerhetstjenesten.felles.utils.extensions

import org.slf4j.MDC

object DomainExtensions {
    const val UTILGJENGELIG = "N/A"

    fun String.maskFnr() =
        when (length) {
            11 -> replaceRange(4, 11, "*******")
            13 -> replaceRange(6, 13, "*******")
            else -> this
        }

    fun requireDigits(verdi: String, len: Int) {
        require(verdi.all { it.isDigit() }) { "Ugyldig(e) tegn i $verdi, forventet $len siffer" }
        require(verdi.length == len) { "Ugyldig lengde ${verdi.length} for $verdi, forventet $len siffer" }
    }

    fun String.upcase() = this.replaceFirstChar { it.uppercaseChar() }

    inline fun <T> withMDC(vararg pairs: Pair<String, String>, block: () -> T) =
        withMDC(verdier = pairs.toMap(), block = block)

    inline fun <T> withMDC(verdier: Map<String, String>, block: () -> T) =
        try {
            verdier.forEach { (key, value) ->
                MDC.put(key, value)
            }
            block()
        } finally {
            verdier.forEach { (key, _) ->
                MDC.remove(key)
            }
        }

}
