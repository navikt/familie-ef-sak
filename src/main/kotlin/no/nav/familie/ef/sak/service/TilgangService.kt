package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.ManglerTilgang
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class TilgangService(private val integrasjonerClient: FamilieIntegrasjonerClient,
                     private val personService: PersonService,
                     private val behandlingService: BehandlingService,
                     private val fagsakService: FagsakService) {


    fun validerTilgangTilPersonMedBarn(personIdent: String) {
        val person = personService.hentPersonMedRelasjoner(personIdent)

        integrasjonerClient.sjekkTilgangTilPersoner(person.identifikatorer()).forEach {
            if (!it.harTilgang) {
                throw ManglerTilgang("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang til ${personIdent} eller dets barn")
            }
        }
    }

    fun validerTilgangTilBehandling(behandlingId: UUID) {
        val fagsakId = behandlingService.hentBehandling(behandlingId).fagsakId
        val personIdent = fagsakService.hentFagsak(fagsakId).hentAktivIdent()

        validerTilgangTilPersonMedBarn(personIdent)
    }

}