package no.nav.familie.ef.sak.api.journalføring

import no.nav.familie.ef.sak.repository.domain.BehandlingType
import java.util.*

data class JournalføringRequest(val dokumentTitler: Map<String, String>? = null,
                                val fagsakId: UUID,
                                val oppgaveId: String,
                                val behandling: JournalFøringBehandlingRequest)

data class JournalFøringBehandlingRequest(val behandlingsId: UUID? = null, val behandlingType: BehandlingType? = null)