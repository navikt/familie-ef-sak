package no.nav.familie.ef.sak.api.gui.dto

import java.time.LocalDate

data class AnnenForelder(val fødselsnummerAnnenForelder: String,
                         val begrunnelseIkkeOppgittAnnenForelder: String,
                         val bostedsland: String,
                         val harForeldreneBoddSammen: Boolean,
                         val fraflyttingsdato: LocalDate,
                         val foreldresKontakt: String,
                         val næreBoforhold: NæreBoforhold,
                         val kanSøkerAnsesÅHaAleneomsorgen: Boolean,
                         val aleneomsorgBegrunnelse: String,
                         val adresser: List<Adresse>)