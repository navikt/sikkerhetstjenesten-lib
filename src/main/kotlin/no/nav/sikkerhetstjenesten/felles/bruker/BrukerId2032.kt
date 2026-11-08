package no.nav.sikkerhetstjenesten.felles.bruker

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.felles.NoCoverageAnalysis
import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions.maskFnr

data class BrukerId2032(@JsonValue val verdi: String) : Comparable<BrukerId2032> {
    init {
        require(verdi.length == BRUKERID_LENGTH && verdi.all { it.isDigit() }) {
            "BrukerId2032 må bestå av 11 siffer"
        }
        require(kontrollrest(verdi, W1) in LOVLIGE) {
            "Første kontrollsiffer er ugyldig etter kontrollberegningen fra 2032"
        }
        require(kontrollrest(verdi, W2) == 0) {
            "Andre kontrollsiffer er ugyldig"
        }
    }

    companion object {
        private val LOVLIGE = 0..3
        const val BRUKERID_LENGTH = 11
        private val W1 = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
        private val W2 = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

        fun parse(verdi: String)  =
            runCatching {
                BrukerId2032(verdi)
            }.getOrNull()

        private fun kontrollrest(verdi: String, vekter: IntArray): Int =
            (vekter.indices.sumOf {
                verdi[it].digitToInt() * vekter[it] } +
                verdi[vekter.size].digitToInt()) % BRUKERID_LENGTH
    }

    override fun compareTo(other: BrukerId2032) = verdi.compareTo(other.verdi)

    @NoCoverageAnalysis
    override fun toString() = verdi.maskFnr()
}
