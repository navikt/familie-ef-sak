package no.nav.familie.ef.sak.oppgave

import java.time.LocalDate

data class VurderHenvendelseOppgaveDto(
    val vurderHenvendelseOppgave: OppgaveSubtype,
    val datoOpprettet: LocalDate
)
