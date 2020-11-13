package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import java.time.LocalDate

data class GrensesnittavstemmingPayload(val fraDato: LocalDate, val stønadstype: Stønadstype)