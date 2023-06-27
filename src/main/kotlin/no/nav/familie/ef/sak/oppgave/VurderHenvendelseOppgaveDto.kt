package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.behandling.dto.VurderHenvendelseOppgaveSubtype
import java.time.LocalDate

data class VurderHenvendelseOppgaveDto(
    val vurderHenvendelsOppgave: VurderHenvendelseOppgaveSubtype,
    val datoOpprettet: LocalDate,
)
