package no.nav.familie.ef.sak.journalføring.dto

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import java.time.LocalDate
import java.util.UUID

data class JournalføringRequest(
    val dokumentTitler: Map<String, String>? = null,
    val fagsakId: UUID,
    val oppgaveId: String,
    val behandling: JournalføringBehandling,
    val journalførendeEnhet: String,
    val barnSomSkalFødes: List<BarnSomSkalFødes> = emptyList()
)

data class BarnSomSkalFødes(val fødselTerminDato: LocalDate) {

    fun tilBehandlingBarn(behandlingId: UUID): BehandlingBarn = BehandlingBarn(
        behandlingId = behandlingId,
        søknadBarnId = null,
        personIdent = null,
        navn = null,
        fødselTermindato = this.fødselTerminDato
    )
}

data class JournalføringTilNyBehandlingRequest(
    val fagsakId: UUID,
    val behandlingstype: BehandlingType
)

fun JournalføringRequest.valider() {
    feilHvis(this.behandling.behandlingsId != null && barnSomSkalFødes.isNotEmpty()) {
        "Kan ikke sende inn barn når man journalfører på en eksisterende behandling"
    }
    feilHvis(this.behandling.behandlingsId != null && this.behandling.årsak != null) {
        "Kan ikke sende inn årsak på en eksisterende behandling"
    }
    feilHvis(this.behandling.årsak != null && this.behandling.årsak != BehandlingÅrsak.PAPIRSØKNAD) {
        "Har ikke støtte for andre årsaker enn papirsøknad"
    }
    feilHvis(this.behandling.årsak != BehandlingÅrsak.PAPIRSØKNAD && barnSomSkalFødes.isNotEmpty()) {
        "Årsak må være satt til papirsøknad hvis man sender inn barn som skal fødes"
    }
}

fun JournalføringRequest.skalJournalførePåEksisterendeBehandling(): Boolean = this.behandling.behandlingsId != null

data class JournalføringBehandling(
    val behandlingsId: UUID? = null,
    val behandlingstype: BehandlingType? = null,
    val årsak: BehandlingÅrsak? = null
)
