package no.nav.familie.ef.sak.api.journalføring;
import no.nav.familie.ef.sak.service.JournalføringService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/journalpost")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalføringController(val journalføringService: JournalføringService)  {

    @GetMapping("/{journalpostId}")
    fun hentJournalPost(@PathVariable journalpostId: String): Ressurs<Journalpost> {
        // TODO: Tilgangskontroll til person
        return Ressurs.success(journalføringService.hentJournalpost(journalpostId))
    }

    @GetMapping("/{journalpostId}/dokument/{dokumentInfoId}")
    fun hentDokument(@PathVariable journalpostId: String, @PathVariable dokumentInfoId: String): Ressurs<ByteArray> {
        // TODO: Tilgangskontroll til person
        return Ressurs.success(journalføringService.hentDokument(journalpostId, dokumentInfoId));
    }

    @PostMapping("/{journalpostId}")
    fun fullførJournalpost(@PathVariable journalpostId: String, @RequestBody journalføringRequest: JournalføringRequest): Ressurs<Long>{
        // TODO: Tilgangskontroll til person
        return Ressurs.success(journalføringService.fullførJournalpost(journalføringRequest, journalpostId))
    }

}
