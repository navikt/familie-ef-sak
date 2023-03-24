package no.nav.familie.ef.sak.felles.dto

import java.time.LocalDate

@Deprecated("Bruk periode fra kontrakter felles.", ReplaceWith("no.nav.familie.kontrakter.felles.Periode"))
data class Periode(
    val fradato: LocalDate,
    val tildato: LocalDate,
) {

    init {
        require(fradato <= tildato) { "Fradato må komme før tildato i enn periode." }
    }
}
