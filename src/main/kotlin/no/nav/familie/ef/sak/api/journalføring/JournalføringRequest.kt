package no.nav.familie.ef.sak.api.journalføring

import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import java.util.*

data class JournalføringRequest (val dokumentTitler: Map<String, String>, val fagsakId: UUID, val oppgaveId: String, val behandling: JournalFøringBehandlingRequest )

data class JournalFøringBehandlingRequest (val behandlingsId: UUID?, val behandlingType: BehandlingType?)