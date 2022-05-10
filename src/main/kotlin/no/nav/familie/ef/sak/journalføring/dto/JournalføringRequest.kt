package no.nav.familie.ef.sak.journalføring.dto

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import java.time.LocalDate
import java.util.UUID

data class JournalføringRequest(val dokumentTitler: Map<String, String>? = null,
                                val fagsakId: UUID,
                                val oppgaveId: String,
                                val behandling: JournalføringBehandling,
                                val journalførendeEnhet: String,
                                val terminbarn: List<ManueltInntastetTerminbarn>
                                )

data class ManueltInntastetTerminbarn(val fødselTerminDato: LocalDate,
                                      val navn: String? = null) {

    fun tilBehandlingBarn(behandlingId: UUID): BehandlingBarn = BehandlingBarn(
            behandlingId = behandlingId,
            søknadBarnId = null,
            personIdent = null,
            navn = this.navn,
            fødselTermindato = this.fødselTerminDato)
}


data class JournalføringTilNyBehandlingRequest(val fagsakId: UUID,
                                               val behandlingstype: BehandlingType)

fun JournalføringRequest.valider() {
    feilHvis(this.behandling.behandlingsId == null && terminbarn.isNotEmpty()) {
        "Kan ikke sende inn terminbarn når man journalfører på en eksisterende behandling"
    }
}

fun JournalføringRequest.skalJournalførePåEksisterendeBehandling(): Boolean = this.behandling.behandlingsId != null

data class JournalføringBehandling(val behandlingsId: UUID? = null,
                                   val behandlingstype: BehandlingType? = null)