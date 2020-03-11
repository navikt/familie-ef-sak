package no.nav.familie.ef.sak.integration.dto.personopplysning.relasjon

import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonIdent
import java.time.LocalDate

class Familierelasjon(val personIdent: PersonIdent,
                      val relasjonsrolle: RelasjonsRolleType,
                      val fødselsdato: LocalDate,
                      val harSammeBosted: Boolean) {

    override fun toString(): String {
        return (javaClass.simpleName
                + "<relasjon=" + relasjonsrolle
                + ", fødselsdato=" + fødselsdato
                + ">")
    }

}