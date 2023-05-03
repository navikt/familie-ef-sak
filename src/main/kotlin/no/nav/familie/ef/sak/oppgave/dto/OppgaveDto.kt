package no.nav.familie.ef.sak.oppgave.dto

import no.nav.familie.kontrakter.felles.Behandlingstema
import java.util.UUID

data class OppgaveDto(val behandlingId: UUID, val gsakOppgaveId: Long)

data class UtdanningOppgaveDto(
    val personnummer: String?,
    val stønadType: Behandlingstema?,
    val oppgavetype: String?,
    val beskrivelse: String?,
)
