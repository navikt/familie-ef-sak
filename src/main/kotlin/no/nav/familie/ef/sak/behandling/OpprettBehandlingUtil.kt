package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import org.springframework.http.HttpStatus

object OpprettBehandlingUtil {

    /**
     * @param behandlingType for ny behandling
     */
    fun validerKanOppretteNyBehandling(
        behandlingType: BehandlingType,
        tidligereBehandlinger: List<Behandling>,
        erMigrering: Boolean = false
    ) {
        val sisteBehandling = tidligereBehandlinger
            .filter { it.resultat != BehandlingResultat.HENLAGT && it.status == BehandlingStatus.FERDIGSTILT }
            .maxByOrNull { it.sporbar.opprettetTid }

        validerTidligereBehandlingerErFerdigstilte(tidligereBehandlinger)
        validerMigreringErRevurdering(behandlingType, erMigrering)

        when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> validerKanOppretteFørstegangsbehandling(sisteBehandling)
            BehandlingType.REVURDERING -> validerKanOppretteRevurdering(sisteBehandling, erMigrering)
        }
    }

    private fun validerMigreringErRevurdering(behandlingType: BehandlingType, erMigrering: Boolean) {
        feilHvis(erMigrering && behandlingType != BehandlingType.REVURDERING) {
            "Det er ikke mulig å lage en migrering av annet enn revurdering"
        }
    }

    private fun validerTidligereBehandlingerErFerdigstilte(tidligereBehandlinger: List<Behandling>) {
        if (tidligereBehandlinger.any { it.status != BehandlingStatus.FERDIGSTILT }) {
            throw ApiFeil("Det finnes en behandling på fagsaken som ikke er ferdigstilt", HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerKanOppretteFørstegangsbehandling(sisteBehandling: Behandling?) {
        if (sisteBehandling == null) return
        brukerfeilHvis(sisteBehandling.type != BehandlingType.FØRSTEGANGSBEHANDLING) {
            "Kan ikke opprette en førstegangsbehandling når forrige behandling ikke er en førstegangsbehandling"
        }
        brukerfeilHvis(sisteBehandling.resultat != BehandlingResultat.HENLAGT) {
            "Kan ikke opprette en førstegangsbehandling når siste behandling ikke er henlagt"
        }
    }

    private fun validerKanOppretteRevurdering(sisteBehandling: Behandling?, erMigrering: Boolean) {
        if (sisteBehandling == null && !erMigrering) {
            throw ApiFeil("Det finnes ikke en tidligere behandling på fagsaken", HttpStatus.BAD_REQUEST)
        }
        if (erMigrering && sisteBehandling != null) {
            throw ApiFeil(
                "Det er ikke mulig å opprette en migrering når det finnes en behandling fra før",
                HttpStatus.BAD_REQUEST
            )
        }
    }
}
