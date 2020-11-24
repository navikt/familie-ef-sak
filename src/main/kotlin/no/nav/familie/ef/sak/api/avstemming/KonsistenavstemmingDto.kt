package no.nav.familie.ef.sak.api.avstemming

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import java.time.LocalDate

data class KonsistenavstemmingDto(val datoForAvstemming: LocalDate, val stønadstype: Stønadstype)