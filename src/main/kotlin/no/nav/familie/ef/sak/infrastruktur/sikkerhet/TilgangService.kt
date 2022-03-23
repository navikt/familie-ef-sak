package no.nav.familie.ef.sak.infrastruktur.sikkerhet

import no.nav.familie.ef.sak.AuditLogger
import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.CustomKeyValue
import no.nav.familie.ef.sak.Sporingsdata
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.integration.dto.Tilgang
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.config.getValue
import no.nav.familie.ef.sak.infrastruktur.exception.ManglerTilgang
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext.hentGrupperFraToken
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering.FORTROLIG
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilgangService(private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
                     private val behandlingService: BehandlingService,
                     private val fagsakService: FagsakService,
                     private val fagsakPersonService: FagsakPersonService,
                     private val rolleConfig: RolleConfig,
                     private val cacheManager: CacheManager,
                     private val auditLogger: AuditLogger) {

    /**
     * Kun ved tilgangskontroll for enskild person, ellers bruk [validerTilgangTilPersonMedBarn]
     */
    fun validerTilgangTilPerson(personIdent: String, event: AuditLoggerEvent) {
        val tilgang = personopplysningerIntegrasjonerClient.sjekkTilgangTilPerson(personIdent)
        auditLogger.log(Sporingsdata(event, personIdent, tilgang))
        if (!tilgang.harTilgang) {
            throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                 "har ikke tilgang til $personIdent")
        }
    }

    fun validerTilgangTilPersonMedBarn(personIdent: String, event: AuditLoggerEvent) {
        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(Sporingsdata(event, personIdent, tilgang))
        if (!tilgang.harTilgang) {
            throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                 "har ikke tilgang til $personIdent eller dets barn")
        }
    }

    fun validerTilgangTilBehandling(behandlingId: UUID, event: AuditLoggerEvent) {
        val personIdent = cacheManager.getValue("behandlingPersonIdent", behandlingId) {
            behandlingService.hentAktivIdent(behandlingId)
        }
        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(Sporingsdata(event, personIdent, tilgang,
                                     custom1 = CustomKeyValue("behandling", behandlingId.toString())))
        if (!tilgang.harTilgang) {
            throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                 "har ikke tilgang til behandling=$behandlingId")
        }
    }

    fun validerTilgangTilBehandling(saksbehandling: Saksbehandling, event: AuditLoggerEvent) {
        val tilgang = harTilgangTilPersonMedRelasjoner(saksbehandling.ident)
        auditLogger.log(Sporingsdata(event,
                                     saksbehandling.ident,
                                     tilgang,
                                     CustomKeyValue("behandling", saksbehandling.id.toString())))
        if (!tilgang.harTilgang) {
            throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                 "har ikke tilgang til behandling=${saksbehandling.id}")
        }
    }

    fun validerTilgangTilFagsak(fagsakId: UUID, event: AuditLoggerEvent) {
        val personIdent = cacheManager.getValue("fagsakIdent", fagsakId) {
            fagsakService.hentAktivIdent(fagsakId)
        }
        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(Sporingsdata(event, personIdent, tilgang, custom1 = CustomKeyValue("fagsak", fagsakId.toString())))
        if (!tilgang.harTilgang) {
            throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                 "har ikke tilgang til fagsak=$fagsakId")
        }
    }

    fun validerTilgangTilFagsakPerson(fagsakPersonId: UUID, event: AuditLoggerEvent) {
        val personIdent = cacheManager.getValue("fagsakPersonIdent", fagsakPersonId) {
            fagsakPersonService.hentAktivIdent(fagsakPersonId)
        }
        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(Sporingsdata(event,
                                     personIdent,
                                     tilgang,
                                     custom1 = CustomKeyValue("fagsakPersonId", fagsakPersonId.toString())))
        if (!tilgang.harTilgang) {
            throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                 "har ikke tilgang til fagsakPerson=$fagsakPersonId")
        }
    }

    private fun harTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang {
        return harSaksbehandlerTilgang("validerTilgangTilPersonMedBarn", personIdent) {
            personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(personIdent)
        }
    }

    fun validerHarSaksbehandlerrolle() {
        validerTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)
    }

    fun validerHarBeslutterrolle() {
        validerTilgangTilRolle(BehandlerRolle.BESLUTTER)
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
     * Filtrerer data basert på om man har tilgang til den eller ikke
     * Filtrer ikke på egen ansatt
     */
    fun <T> filtrerUtFortroligDataForRolle(values: List<T>, fn: (T) -> Adressebeskyttelse?): List<T> {
        val grupper = hentGrupperFraToken()
        val kode6gruppe = grupper.contains(rolleConfig.kode6)
        val kode7Gruppe = grupper.contains(rolleConfig.kode7)
        return values.filter {
            when (fn(it)?.gradering) {
                FORTROLIG -> kode7Gruppe
                STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND -> kode6gruppe
                else -> (!kode6gruppe)
            }
        }
    }

    /**
     * Sjekker cache om tilgangen finnes siden tidligere, hvis ikke hentes verdiet med [hentVerdi]
     * Resultatet caches sammen med identen for saksbehandleren på gitt [cacheName]
     * @param cacheName navnet på cachen
     * @param verdi verdiet som man ønsket å hente cache for, eks behandlingId, eller personIdent
     */
    private fun <T> harSaksbehandlerTilgang(cacheName: String, verdi: T, hentVerdi: () -> Tilgang): Tilgang {
        val cache = cacheManager.getCache(cacheName) ?: error("Finner ikke cache=$cacheName")
        return cache.get(Pair(verdi, SikkerhetContext.hentSaksbehandler(true))) {
            hentVerdi()
        } ?: error("Finner ikke verdi fra cache=$cacheName")
    }

    fun validerSaksbehandler(saksbehandler: String): Boolean {
        return SikkerhetContext.hentSaksbehandler() == saksbehandler
    }
}