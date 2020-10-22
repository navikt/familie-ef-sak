package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import java.time.LocalDate

data class DummyIverksettingDTO(val personIdent: String,
                                val beløp: Int,
                                val stønadFom: LocalDate,
                                val stønadTom: LocalDate,
                                val stønadstype: Stønadstype)