package no.nav.familie.ef.sak.journalføring.dto

import java.time.LocalDate
import java.util.UUID

data class JournalføringKlageRequest(
    val dokumentTitler: Map<String, String>? = null,
    val fagsakId: UUID,
    val oppgaveId: String,
    val behandling: JournalføringKlageBehandling,
    val journalførendeEnhet: String
)

data class JournalføringKlageBehandling(
    val behandlingId: UUID? = null,
    val mottattDato: LocalDate? = null
)

fun JournalføringKlageRequest.skalJournalførePåEksisterendeBehandling(): Boolean = this.behandling.behandlingId != null
