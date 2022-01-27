package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.fagsak.dto.Søkeresultat
import no.nav.familie.ef.sak.fagsak.dto.SøkeresultatPerson
import no.nav.familie.ef.sak.fagsak.dto.SøkeresultatUtenFagsak
import no.nav.familie.ef.sak.felles.dto.PersonIdentDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/sok"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SøkController(private val søkService: SøkService, private val tilgangService: TilgangService) {

    @PostMapping("", "/person")
    fun søkPerson(@RequestBody personIdentRequest: PersonIdentDto): Ressurs<Søkeresultat> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdentRequest.personIdent, AuditLoggerEvent.ACCESS)
        return Ressurs.success(søkService.søkPerson(personIdentRequest.personIdent))
    }

    @PostMapping("/person/uten-fagsak")
    fun søkPersonUtenFagsak(@RequestBody personIdentRequest: PersonIdentDto): Ressurs<SøkeresultatUtenFagsak> {
        tilgangService.validerTilgangTilPerson(personIdentRequest.personIdent, AuditLoggerEvent.ACCESS)

        return Ressurs.success(søkService.søkPersonUtenFagsak(personIdentRequest.personIdent))
    }

    @GetMapping("{behandlingId}/samme-adresse")
    fun søkPersonerMedSammeAdressePåBehandling(@PathVariable behandlingId: UUID): Ressurs<SøkeresultatPerson> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(søkService.søkEtterPersonerMedSammeAdressePåBehandling(behandlingId))
    }

    @GetMapping("fagsak/{fagsakId}/samme-adresse")
    fun søkPersonerMedSammeAdressePåFagsak(@PathVariable fagsakId: UUID): Ressurs<SøkeresultatPerson> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(søkService.søkEtterPersonerMedSammeAdressePåFagsak(fagsakId))
    }
}