package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
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
        if (behandlingType == BehandlingType.TEKNISK_OPPHØR) {
            validerTekniskOpphør(sisteBehandling, tidligereBehandlinger)
        }
    }

    fun sistIverksatteBehandling(behandlinger: List<Behandling>): Behandling? {
        return behandlinger
                .filter(this::erIverksatt)
                .maxByOrNull { it.sporbar.opprettetTid }
    }

    /**
     * Vi ønsker å peke den nye behandlingen til en behandling som er iverksatt og som påvirker andeler
     *  [BehandlingResultat.INNVILGET] ELLER [BehandlingResultat.OPPHØRT]
     *  [BehandlingResultat.AVSLÅTT] og [BehandlingResultat.ANNULLERT] er ikke intressante for det formålet
     */
    private fun erIverksatt(behandling: Behandling) =
            behandling.type != BehandlingType.BLANKETT &&
            setOf(BehandlingResultat.INNVILGET, BehandlingResultat.OPPHØRT).contains(behandling.resultat) &&
            behandling.status == BehandlingStatus.FERDIGSTILT

    private fun validerTekniskOpphør(sisteBehandling: Behandling?, tidligereBehandlinger: List<Behandling>) {
        if (sisteBehandling == null) {
            throw ApiFeil("Det finnes ikke en tidligere behandling for fagsaken", HttpStatus.BAD_REQUEST)
        }
        val sistIverksatteBehandling = sistIverksatteBehandling(tidligereBehandlinger)
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