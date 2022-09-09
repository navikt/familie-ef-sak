package no.nav.familie.ef.sak.ekstern.journalføring

import no.nav.familie.ef.sak.felles.util.FnrUtil.validerIdent
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.journalføring.AutomatiskJournalføringRequest
import no.nav.familie.kontrakter.ef.journalføring.AutomatiskJournalføringResponse
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/automatisk-journalforing"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class AutomatiskJournalføringController(
    private val automatiskJournalføringService: AutomatiskJournalføringService
) {

    /**
     * Skal bare brukes av familie-ef-mottak for å vurdere om en journalføring skal automatisk ferdigstilles
     * eller manuelt gjennomgås.
     */
    @PostMapping("kan-opprette-forstegangsbehandling")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun kanOppretteFørstegangsbehandling(
        @RequestBody personIdent: PersonIdent,
        @RequestParam type: StønadType
    ): Ressurs<Boolean> {
        if (!SikkerhetContext.kallKommerFraFamilieEfMottak()) {
            throw Feil(message = "Kallet utføres ikke av en autorisert klient", httpStatus = HttpStatus.UNAUTHORIZED)
        }
        validerIdent(personIdent.ident)
        return Ressurs.success(automatiskJournalføringService.kanOppretteFørstegangsbehandling(personIdent.ident, type))
    }

    /**
     * Skal bare brukes av familie-ef-mottak for å automatisk journalføre
     */
    @PostMapping("journalfor")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun automatiskJournalfør(
        @RequestBody request: AutomatiskJournalføringRequest
    ): Ressurs<AutomatiskJournalføringResponse> {
        if (!SikkerhetContext.kallKommerFraFamilieEfMottak()) {
            throw Feil(message = "Kallet utføres ikke av en autorisert klient", httpStatus = HttpStatus.UNAUTHORIZED)
        }
        validerIdent(request.personIdent)
        return Ressurs.success(
            automatiskJournalføringService.automatiskJournalførTilFørstegangsbehandling(
                journalpostId = request.journalpostId,
                personIdent = request.personIdent,
                stønadstype = request.stønadstype,
                mappeId = request.mappeId
            )
        )
    }
}
