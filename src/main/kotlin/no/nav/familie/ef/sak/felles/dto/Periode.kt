package no.nav.familie.ef.sak.felles.dto

import java.time.LocalDate
import java.time.Period

data class Periode(val fradato: LocalDate, val tildato: LocalDate, val gyldig: Boolean? = null) {

    fun inneholder(date: LocalDate): Boolean {
        return (!date.isBefore(fradato) && !date.isAfter(tildato))
    }

    fun inneholder(periode: Periode): Boolean {
        return (periode.fradato.isAfter(this.fradato) && periode.tildato.isBefore(this.tildato))
    }

    fun omsluttesAv(periode: Periode): Boolean {
        return (!periode.fradato.isAfter(this.fradato) && !periode.tildato.isBefore(this.tildato))
    }

    fun overlapperIStartenAv(periode: Periode) =
            this.fradato.isBefore(periode.fradato)
            && this.tildato.isAfter(periode.fradato)
            && this.tildato.isBefore(periode.tildato)

    private val lengde: Period = Period.between(fradato, tildato)
}