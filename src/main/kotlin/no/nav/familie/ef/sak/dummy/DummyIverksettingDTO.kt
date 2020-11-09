package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import java.time.LocalDate

data class DummyIverksettingDTO(val personIdent: String = "12345678910",
                                val beløp: Int = 1000,
                                val stønadFom: LocalDate = LocalDate.now().withDayOfMonth(1),
                                val stønadTom: LocalDate = LocalDate.now().plusMonths(2).withDayOfMonth(28),
                                val behandlingstype: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                                val stønadstype: Stønadstype = Stønadstype.OVERGANGSSTØNAD)