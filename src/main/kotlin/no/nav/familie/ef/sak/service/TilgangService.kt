package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.ManglerTilgang
import no.nav.familie.ef.sak.config.RolleConfig
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.service.steg.BehandlerRolle
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import java.util.*

@Service
class TilgangService(private val integrasjonerClient: FamilieIntegrasjonerClient,
                     private val personService: PersonService,
                     private val behandlingService: BehandlingService,
                     private val fagsakService: FagsakService,
                     private val rolleConfig: RolleConfig) {

    fun validerTilgangTilPersonMedBarn(personIdent: String) {
        val barnOgForeldre = personService.hentIdenterForBarnOgForeldre(forelderIdent = personIdent)

        integrasjonerClient.sjekkTilgangTilPersoner(barnOgForeldre).forEach {
            if (!it.harTilgang) {
                throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                     "har ikke tilgang til ${personIdent} eller dets barn")
            }
        }
    }

    fun validerTilgangTilBehandling(behandlingId: UUID) {
        val fagsakId = behandlingService.hentBehandling(behandlingId).fagsakId
        val personIdent = fagsakService.hentFagsak(fagsakId).hentAktivIdent()

        validerTilgangTilPersonMedBarn(personIdent)
    }

    fun validerHarSaksbehandlerrolle() {
        validerTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)
    }

    fun validerTilgangTilRolle(minimumsrolle: BehandlerRolle) {
        if (!SikkerhetContext.harTilgangTilGittRolle(rolleConfig, minimumsrolle)) {
            throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang " +
                                 "til å utføre denne operasjonen som krever minimumsrolle ${minimumsrolle}")
        }
    }
}