package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import org.springframework.http.HttpStatus

object OpprettBehandlingUtil {

    /**
     * @param behandlingType for ny behandling
     */
    fun validerKanOppretteNyBehandling(behandlingType: BehandlingType,
                                       tidligereBehandlinger: List<Behandling>,
                                       sistIverksatteBehandling: Behandling?) {
        val sisteBehandling = tidligereBehandlinger
                .filter { it.resultat != BehandlingResultat.ANNULLERT && it.resultat != BehandlingResultat.AVSLÅTT && it.status == BehandlingStatus.FERDIGSTILT }
                .maxByOrNull { it.sporbar.opprettetTid }

        validerTidligereBehandlingerErFerdigstilte(tidligereBehandlinger)

        when (behandlingType) {
            BehandlingType.BLANKETT -> validerKanOppretteBlankett(tidligereBehandlinger)
            BehandlingType.FØRSTEGANGSBEHANDLING -> validerKanOppretteFørstegangsbehandling(sisteBehandling)
            BehandlingType.REVURDERING -> validerKanOppretteRevurdering(sisteBehandling)
            BehandlingType.TEKNISK_OPPHØR -> validerTekniskOpphør(sisteBehandling, sistIverksatteBehandling)
        }
    }

    private fun validerTekniskOpphør(sisteBehandling: Behandling?,
                                     sistIverksatteBehandling: Behandling?) {
        if (sisteBehandling == null) {
            throw ApiFeil("Det finnes ikke en tidligere behandling for fagsaken", HttpStatus.BAD_REQUEST)
        }
        if (sistIverksatteBehandling != sisteBehandling) {
            throw ApiFeil("Siste behandlingen må være iverksatt for å kunne utføre teknisk opphør", HttpStatus.BAD_REQUEST)
        }
        if (sistIverksatteBehandling.type == BehandlingType.TEKNISK_OPPHØR) {
            throw ApiFeil("Kan ikke opphøre en allerede opphørt behandling", HttpStatus.BAD_REQUEST)
        }
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