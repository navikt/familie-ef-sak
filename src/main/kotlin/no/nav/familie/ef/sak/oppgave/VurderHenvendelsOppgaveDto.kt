package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.behandling.dto.VurderHenvendelseOppgavetype
import java.time.LocalDate

data class VurderHenvendelsOppgaveDto(
    val vurderHenvendelsOppgave: VurderHenvendelseOppgavetype,
    val datoOpprettet: LocalDate,
)
