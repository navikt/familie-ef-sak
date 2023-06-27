package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.behandling.dto.VurderHenvendelseOppgaveSubtype
import java.time.LocalDate

data class VurderHenvendelsOppgaveDto(
    val vurderHenvendelsOppgave: VurderHenvendelseOppgaveSubtype,
    val datoOpprettet: LocalDate,
)
