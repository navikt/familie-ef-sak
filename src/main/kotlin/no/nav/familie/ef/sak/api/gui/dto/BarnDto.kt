package no.nav.familie.ef.sak.api.gui.dto

import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import java.time.LocalDate

data class BarnDto(val navn: String,
val fødselsnummer: String,
val termindato: LocalDate,
val fødselsnummerAnnenForelder,
val aleneomsorg: Boolean,
val søkersRelasjonTilBarnet: String,
val harSøkerOvertattForeldreansvaret: Boolean,
val begrunnelseOvertattForeldreansvar: String,
val borBarnetHosSøker: String,
val skalBarnetBoHosSøker: String,
)