package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.fagsak.dto.Søkeresultat
import no.nav.familie.ef.sak.fagsak.dto.SøkeresultatPerson
import no.nav.familie.ef.sak.fagsak.dto.SøkeresultatUtenFagsak
import no.nav.familie.ef.sak.felles.dto.PersonIdentDto
import no.nav.familie.ef.sak.felles.util.FnrUtil.validerIdent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
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
class SøkController(
    private val søkService: SøkService,
    private val personService: PersonService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("", "/person")
    fun søkPerson(
        @RequestBody personIdentRequest: PersonIdentDto,
    ): Ressurs<Søkeresultat> {
        validerPersonIdent(personIdentRequest)
        val personIdenter = hentOgValiderAtIdentEksisterer(personIdentRequest)
        tilgangService.validerTilgangTilPersonMedBarn(personIdentRequest.personIdent, AuditLoggerEvent.ACCESS)
        return Ressurs.success(søkService.søkPerson(personIdenter))
    }

    @GetMapping("/person/fagsak-ekstern/{eksternFagsakId}")
    fun søkPerson(
        @PathVariable eksternFagsakId: Long,
    ): Ressurs<Søkeresultat> {
        val søkeresultat = søkService.søkPersonForEksternFagsak(eksternFagsakId)
        søkeresultat.fagsakPersonId?.let {
            tilgangService.validerTilgangTilFagsakPerson(it, AuditLoggerEvent.ACCESS)
        }
        return Ressurs.success(søkeresultat)
    }

    @PostMapping("/person/uten-fagsak")
    fun søkPersonUtenFagsak(
        @RequestBody personIdentRequest: PersonIdentDto,
    ): Ressurs<SøkeresultatUtenFagsak> {
        validerPersonIdent(personIdentRequest)
        tilgangService.validerTilgangTilPerson(personIdentRequest.personIdent, AuditLoggerEvent.ACCESS)

        return Ressurs.success(søkService.søkPersonUtenFagsak(personIdentRequest.personIdent))
    }

    @GetMapping("{behandlingId}/samme-adresse")
    fun søkPersonerMedSammeAdressePåBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<SøkeresultatPerson> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(søkService.søkEtterPersonerMedSammeAdressePåBehandling(behandlingId))
    }

    @GetMapping("fagsak-person/{fagsakPersonId}/samme-adresse")
    fun søkPersonerMedSammeAdressePåFagsakPerson(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<SøkeresultatPerson> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(søkService.søkEtterPersonerMedSammeAdressePåFagsakPerson(fagsakPersonId))
    }

    private fun validerPersonIdent(personIdentRequest: PersonIdentDto) {
        validerIdent(personIdentRequest.personIdent)
    }

    private fun hentOgValiderAtIdentEksisterer(personIdentRequest: PersonIdentDto): PdlIdenter = personService.hentPersonIdenter(personIdentRequest.personIdent)
}
