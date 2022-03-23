package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDate

data class LøpendeBehandling(val stonadstype: StønadType,
                             val dato: LocalDate,
                             val antall: Int,
                             val belop: Long)