package no.nav.familie.ef.sak.api.journalføring;

import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.service.JournalføringService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.BrukerIdType
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/journalpost")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalføringController(private val journalføringService: JournalføringService, private val pdlClient: PdlClient, private val tilgangService: TilgangService) {

    @GetMapping("/{journalpostId}")
    fun hentJournalPost(@PathVariable journalpostId: String): Ressurs<JournalføringResponse> {
        val (journalpost, personIdent) = finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent)
        return Ressurs.success(JournalføringResponse(journalpost, personIdent))
    }

    @GetMapping("/{journalpostId}/dokument/{dokumentInfoId}")
    fun hentDokument(@PathVariable journalpostId: String, @PathVariable dokumentInfoId: String): Ressurs<ByteArray> {
        val (_, personIdent) = finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent)
        return Ressurs.success(journalføringService.hentDokument(journalpostId, dokumentInfoId))
    }

    @PostMapping("/{journalpostId}/fullfor")
    fun fullførJournalpost(@PathVariable journalpostId: String,
                           @RequestBody journalføringRequest: JournalføringRequest
    ): Ressurs<Long> {
        val (_, personIdent) = finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent)
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(journalføringService.fullførJournalpost(journalføringRequest, journalpostId))
    }

    fun finnJournalpostOgPersonIdent(journalpostId: String): Pair<Journalpost, String> {
        val journalpost = journalføringService.hentJournalpost(journalpostId)
        val personIdent = journalpost.bruker?.let {
            when (it.type) {
                BrukerIdType.FNR -> it.id
                BrukerIdType.AKTOERID -> pdlClient.hentPersonident(it.id).hentIdenter.identer.first().ident
                BrukerIdType.ORGNR -> error("Kan ikke hente journalpost= ${journalpostId} for orgnr")
            }
        } ?: error("Kan ikke hente journalpost= ${journalpostId} uten bruker")
        return Pair(journalpost, personIdent)
    }
}
