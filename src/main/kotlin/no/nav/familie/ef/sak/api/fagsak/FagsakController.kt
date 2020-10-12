package no.nav.familie.ef.sak.api.fagsak

import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.validering.FagsakPersontilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.ConstraintViolationException

@RestController
@RequestMapping(path = ["/api/fagsak"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(private val fagsakService: FagsakService) {


    @ExceptionHandler(ConstraintViolationException::class)
    fun handleRestClientResponseException(e: ConstraintViolationException): Ressurs<ConstraintViolationException> {
        return Ressurs.ikkeTilgang(melding = e.message ?: "Ikke tilgang til å hente ut fagsak")
    }

    @PostMapping
    fun hentFagsakForPerson(@FagsakPersontilgangConstraint @RequestBody fagsakRequest: FagsakRequest): Ressurs<FagsakDto> {
        return Ressurs.success(fagsakService.hentEllerOpprettFagsak(fagsakRequest.personIdent, fagsakRequest.stønadstype))
    }

}