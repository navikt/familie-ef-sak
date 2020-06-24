package no.nav.familie.ef.sak.api.gui.dto

import java.time.LocalDate

data class Barn(val navn: String,
                val fødselsnummer: String?,
                val termindatoFødselsdato: LocalDate?,
                val begrunnelseIkkeOppgittAnnenForelder: String?,
                val annenForelder: AnnenForelder?,
                val aleneomsorg: Boolean,
                val søkersRelasjonTilBarnet: String,
                val skalBoBorHosSøker: Boolean,
                val deltBosted: DeltBosted,
                val fraRegister: Boolean)

