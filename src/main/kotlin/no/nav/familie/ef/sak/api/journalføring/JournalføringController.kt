package no.nav.familie.ef.sak.api.journalføring;

import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.service.JournalføringService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.BrukerIdType
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/journalpost")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalføringController(val journalføringService: JournalføringService, val pdlClient: PdlClient) {

    @GetMapping("/{journalpostId}")
    fun hentJournalPost(@PathVariable journalpostId: String): Ressurs<JournalføringResponse> {
        // TODO: Tilgangskontroll til person
        val journalpost = journalføringService.hentJournalpost(journalpostId)
        val personIdent = journalpost.bruker?.let {
            when (it.type) {
                BrukerIdType.FNR -> it.id
                BrukerIdType.AKTOERID -> pdlClient.hentPersonident(it.id).hentIdenter.identer.first().ident
                BrukerIdType.ORGNR -> error("Kan ikke hente journalpost= ${journalpostId} for orgnr")
            }
        } ?: error("Kan ikke hente journalpost= ${journalpostId} uten bruker")
        return Ressurs.success(JournalføringResponse(journalpost, personIdent))
    }

    @GetMapping("/{journalpostId}/dokument/{dokumentInfoId}")
    fun hentDokument(@PathVariable journalpostId: String, @PathVariable dokumentInfoId: String): Ressurs<ByteArray> {
        // TODO: Tilgangskontroll til person
        return Ressurs.success(journalføringService.hentDokument(journalpostId, dokumentInfoId));
    }

    @PostMapping("/{journalpostId}/fullfor")
    fun fullførJournalpost(@PathVariable journalpostId: String,
                           @RequestBody journalføringRequest: JournalføringRequest,
                           @RequestParam(name = "journalfoerendeEnhet") journalførendeEnhet: String): Ressurs<Long> {
        // TODO: Tilgangskontroll til person
        return Ressurs.success(journalføringService.fullførJournalpost(journalføringRequest, journalpostId, journalførendeEnhet))
    }

}
