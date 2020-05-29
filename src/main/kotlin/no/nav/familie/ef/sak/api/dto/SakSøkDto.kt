package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate
import java.util.*

data class SakSøkDto(val sakId: UUID,
                     val personIdent: String,
                     val navn: NavnDto,
                     val kjønn: Kjønn,
                     val adressebeskyttelse: Adressebeskyttelse,
                     val folkeregisterpersonstatus: Folkeregisterpersonstatus?,
                     val dødsdato: LocalDate?)
