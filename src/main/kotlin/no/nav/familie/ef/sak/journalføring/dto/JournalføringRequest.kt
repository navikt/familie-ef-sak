package no.nav.familie.ef.sak.journalføring.dto

import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import java.util.UUID

data class JournalføringRequest(val dokumentTitler: Map<String, String>? = null,
                                val fagsakId: UUID,
                                val oppgaveId: String,
                                val behandling: JournalføringBehandling,
                                val navIdent: String,
                                val journalførendeEnhet: String)


data class JournalføringTilNyBehandlingRequest(val fagsakId: UUID,
                                      val behandlingstype: BehandlingType,
                                      val navIdent: String)

fun JournalføringRequest.skalJournalførePåEksisterendeBehandling(): Boolean = this.behandling.behandlingsId != null

data class JournalføringBehandling(val behandlingsId: UUID? = null,
                                   val behandlingstype: BehandlingType? = null)