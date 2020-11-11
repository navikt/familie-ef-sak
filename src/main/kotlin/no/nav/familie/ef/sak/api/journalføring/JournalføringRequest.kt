package no.nav.familie.ef.sak.api.journalføring

import no.nav.familie.ef.sak.repository.domain.BehandlingType
import java.util.*

data class JournalføringRequest(val dokumentTitler: Map<String, String>? = null,
                                val fagsakId: UUID,
                                val oppgaveId: String,
                                val behandling: JournalføringBehandling,
                                val navIdent: String,
                                val journalførendeEnhet: String)

data class JournalføringBehandling(val behandlingsId: UUID? = null,
                                   val behandlingstype: BehandlingType? = null)