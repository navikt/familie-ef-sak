package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import java.time.LocalDate

data class LøpendeBehandling(val stonadstype: Stønadstype, val dato: LocalDate, val antall: Int, val belop: Long)