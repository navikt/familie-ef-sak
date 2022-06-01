package no.nav.familie.ef.sak.felles.dto

import java.time.LocalDate
import java.time.Period

data class Periode(
    val fradato: LocalDate,
    val tildato: LocalDate
) {

    init {
        require(fradato <= tildato) { "Fradato må komme før tildato i enn periode." }
    }

    fun omslutter(date: LocalDate): Boolean {
        return date in fradato..tildato
    }

    fun omslutter(annen: Periode): Boolean {
        return annen.fradato >= fradato && annen.tildato <= tildato
    }

    fun overlapper(annen: Periode): Boolean {
        return omslutter(annen.fradato) || omslutter(annen.tildato) || annen.omslutter(fradato)
    }

    fun omsluttesAv(annen: Periode): Boolean {
        return annen.fradato <= fradato && annen.tildato >= tildato
    }

    fun overlapperIStartenAv(annen: Periode) =
        fradato <= annen.fradato &&
            tildato > annen.fradato &&
            tildato < annen.tildato

    fun overlapperISluttenAv(annen: Periode) =
        fradato > annen.fradato &&
            fradato < annen.tildato &&
            tildato >= annen.tildato

    private val lengde: Period = Period.between(fradato, tildato)
}
