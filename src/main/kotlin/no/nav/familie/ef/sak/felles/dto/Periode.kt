package no.nav.familie.ef.sak.felles.dto

import java.time.LocalDate
import java.time.Period

data class Periode(val fradato: LocalDate,
                   val tildato: LocalDate) {

    init {
        require(fradato <= tildato) {"Fradato må kommer før tildato i enn periode."}
    }

    fun inneholder(date: LocalDate): Boolean {
        return date in fradato..tildato
    }

    fun inneholder(annen: Periode): Boolean {
        return annen.fradato >= fradato && annen.tildato <= tildato
    }

    fun overlapper(annen: Periode): Boolean {
        return inneholder(annen.fradato) || inneholder(annen.tildato) || annen.inneholder(fradato)
    }

    fun omsluttesAv(annen: Periode): Boolean {
        return annen.fradato <= fradato && annen.tildato >= tildato
    }

    fun overlapperIStartenAv(annen: Periode) =
            fradato < annen.fradato
            && tildato > annen.fradato
            && tildato < annen.tildato

    fun overlapperISluttenAv(annen: Periode) =
            fradato > annen.fradato
            && fradato < annen.tildato
            && tildato > annen.tildato

    private val lengde: Period = Period.between(fradato, tildato)
}
