package no.nav.familie.ef.sak.util

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import org.springframework.http.HttpStatus

object OpprettBehandlingUtil {

    /**
     * @param behandlingType for ny behandling
     */
    fun validerKanOppretteNyBehandling(behandlingType: BehandlingType,
                                       tidligereBehandlinger: List<Behandling>) {
        val sisteBehandling = tidligereBehandlinger
                .filter { it.resultat != BehandlingResultat.ANNULLERT }
                .maxByOrNull { it.sporbar.opprettetTid }

        validerTidligereBehandlingerErFerdigstilte(tidligereBehandlinger)

        if (behandlingType == BehandlingType.BLANKETT) {
            validerKanOppretteBlankett(tidligereBehandlinger)
        }
        if (behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
            validerKanOppretteFørstegangsbehandling(sisteBehandling)
        }
        if (behandlingType == BehandlingType.REVURDERING) {
            validerKanOppretteRevurdering(sisteBehandling)
        }
    }

    fun sistIverksatteBehandling(behandlinger: List<Behandling>): Behandling? {
        return behandlinger
                .filter { it.type != BehandlingType.BLANKETT }
                .filter { it.resultat != BehandlingResultat.ANNULLERT }
                .filter { it.status == BehandlingStatus.FERDIGSTILT }
                .maxByOrNull { it.sporbar.opprettetTid }
    }

    private fun validerTidligereBehandlingerErFerdigstilte(tidligereBehandlinger: List<Behandling>) {
        if (tidligereBehandlinger.any { it.status != BehandlingStatus.FERDIGSTILT }) {
            throw ApiFeil("Det finnes en behandling på fagsaken som ikke er ferdigstilt", HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerKanOppretteBlankett(tidligereBehandlinger: List<Behandling>) {
        if (tidligereBehandlinger.any { it.type != BehandlingType.BLANKETT }) {
            throw ApiFeil("Kan ikke å opprette blankettbehandling når fagsaken allerede har andre typer behandlinger",
                          HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerKanOppretteFørstegangsbehandling(sisteBehandling: Behandling?) {
        if (sisteBehandling != null &&
            !(sisteBehandling.type == BehandlingType.BLANKETT || sisteBehandling.type == BehandlingType.TEKNISK_OPPHØR)) {
            throw ApiFeil("Siste behandlingen for en førstegangsbehandling må være av typen blankett eller teknisk opphør",
                          HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerKanOppretteRevurdering(sisteBehandling: Behandling?) {
        if (sisteBehandling == null) {
            throw ApiFeil("Det finnes ikke en tidligere behandling på fagsaken", HttpStatus.BAD_REQUEST)
        }
        if (sisteBehandling.type == BehandlingType.BLANKETT) { // Hvordan blir migrerte behandlinger behandlet?
            throw ApiFeil("Siste behandling ble behandlet i infotrygd", HttpStatus.BAD_REQUEST)
        }
        if (sisteBehandling.type == BehandlingType.TEKNISK_OPPHØR) {
            throw ApiFeil("Det er ikke mulig å lage en revurdering når siste behandlingen er teknisk opphør",
                          HttpStatus.BAD_REQUEST)
        }
    }
}