package no.nav.familie.ef.sak.nare.specifications

import no.nav.familie.ef.sak.nare.evaluations.Evaluering
import no.nav.familie.ef.sak.nare.evaluations.Evaluering.Companion.evaluer
import no.nav.familie.ef.sak.nare.evaluations.Resultat


data class Regel<T>(
        val beskrivelse: String,
        val identifikator: String = "",
        val children: List<Regel<T>> = emptyList(),
        val implementasjon: T.() -> Evaluering) {

    fun evaluer(t: T): Evaluering {
        return evaluer(beskrivelse = beskrivelse,
                       identifikator = identifikator,
                       eval = t.implementasjon())
    }

    infix fun og(other: Regel<T>): Regel<T> {
        return Regel(beskrivelse = "$beskrivelse OG ${other.beskrivelse}",
                     children = this.specOrChildren() + other.specOrChildren(),
                     implementasjon = { evaluer(this) og other.evaluer(this) }
        )
    }

    infix fun eller(other: Regel<T>): Regel<T> {
        return Regel(beskrivelse = "$beskrivelse ELLER ${other.beskrivelse}",
                     children = this.specOrChildren() + other.specOrChildren(),
                     implementasjon = {
                                 val evaluer = evaluer(this)
                                 if (evaluer.resultat == Resultat.JA) {
                                     evaluer
                                 } else {
                                     evaluer(this) eller other.evaluer(this)
                                 }
                             }
        )
    }

    fun med(identifikator: String, beskrivelse: String): Regel<T> {
        return this.copy(identifikator = identifikator, beskrivelse = beskrivelse)
    }

    private fun specOrChildren(): List<Regel<T>> =
            if (identifikator.isBlank() && children.isNotEmpty()) children else listOf(this)

}
