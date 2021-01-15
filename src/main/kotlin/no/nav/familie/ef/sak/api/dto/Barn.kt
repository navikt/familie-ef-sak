package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import java.time.LocalDate

data class Barn(val navn: String,
                val fødselsnummer: String?,
                val termindatoFødselsdato: LocalDate?,
                val begrunnelseIkkeOppgittAnnenForelder: EnumTekstverdiMedSvarId?,
                val forelder: Forelder?,
                val skalBoBorHosSøker: Boolean?,
                val deltBosted: DeltBosted,
                val fraRegister: Boolean)

