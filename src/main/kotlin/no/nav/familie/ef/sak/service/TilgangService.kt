package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.ManglerTilgang
import no.nav.familie.ef.sak.config.RolleConfig
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.service.steg.BehandlerRolle
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.*

@Service
class TilgangService(private val integrasjonerClient: FamilieIntegrasjonerClient,
                     private val personService: PersonService,
                     private val behandlingService: BehandlingService,
                     private val fagsakService: FagsakService,
                     private val rolleConfig: RolleConfig,
                     private val cacheManager: CacheManager) {

    fun validerTilgangTilPersonMedBarn(personIdent: String) {
        val harTilgang = harTilgangTilPersonMedBarn(personIdent)
        if (!harTilgang) {
            throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                 "har ikke tilgang til $personIdent eller dets barn")
        }
    }

    private fun harTilgangTilPersonMedBarn(personIdent: String): Boolean {
        return harSaksbehandlerTilgang("validerTilgangTilPersonMedBarn", personIdent) {
            val barnOgForeldre = personService.hentIdenterForBarnOgForeldre(forelderIdent = personIdent)
            integrasjonerClient.sjekkTilgangTilPersoner(barnOgForeldre).all { it.harTilgang }
        }
    }

    fun validerTilgangTilBehandling(behandlingId: UUID) {
        val harTilgang = harSaksbehandlerTilgang("validerTilgangTilBehandling", behandlingId) {
            val personIdent = behandlingService.hentAktivIdent(behandlingId)
            harTilgangTilPersonMedBarn(personIdent)
        }
        if (!harTilgang) {
            throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                 "har ikke tilgang til behandling=$behandlingId")
        }
    }

    fun validerTilgangTilFagsak(fagsakId: UUID) {
        val personIdent = fagsakService.hentAktivIdent(fagsakId)
        validerTilgangTilPersonMedBarn(personIdent)
    }

    fun validerHarSaksbehandlerrolle() {
        validerTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)
    }

    fun validerTilgangTilRolle(minimumsrolle: BehandlerRolle) {
        if (!harTilgangTilRolle(minimumsrolle)) {
            throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang " +
                                 "til å utføre denne operasjonen som krever minimumsrolle $minimumsrolle")
        }
    }

    fun harTilgangTilRolle(minimumsrolle: BehandlerRolle): Boolean {
        return SikkerhetContext.harTilgangTilGittRolle(rolleConfig, minimumsrolle)
    }

    /**
     * Sjekker cache om tilgangen finnes siden tidligere, hvis ikke hentes verdiet med [hentVerdi]
     * Resultatet caches sammen med identen for saksbehandleren på gitt [cacheName]
     * @param cacheName navnet på cachen
     * @param verdi verdiet som man ønsket å hente cache for, eks behandlingId, eller personIdent
     */
    private fun <T> harSaksbehandlerTilgang(cacheName: String, verdi: T, hentVerdi: () -> Boolean): Boolean {
        val cache = cacheManager.getCache(cacheName) ?: error("Finner ikke cache=$cacheName")
        return cache.get(Pair(verdi, SikkerhetContext.hentSaksbehandler(true))) {
            hentVerdi()
        } ?: error("Finner ikke verdi fra cache=$cacheName")
    }

    fun validerSaksbehandler(saksbehandler: String): Boolean {
        return SikkerhetContext.hentSaksbehandler().equals(saksbehandler)
    }
}
