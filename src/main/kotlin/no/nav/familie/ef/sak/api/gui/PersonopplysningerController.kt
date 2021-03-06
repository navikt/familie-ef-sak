package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.api.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.PersonopplysningerService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping(path = ["/api/personopplysninger"], produces = [APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonopplysningerController(private val personopplysningerService: PersonopplysningerService,
                                   private val tilgangService: TilgangService,
                                   private val behandlingService: BehandlingService,
                                   private val fagsakService: FagsakService) {

    @PostMapping
    fun personopplysninger(@RequestBody personIdent: PersonIdentDto): Ressurs<PersonopplysningerDto> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdent.personIdent)
        return Ressurs.success(personopplysningerService.hentPersonopplysninger(personIdent.personIdent))
    }

    @GetMapping("/behandling/{behandlingId}")
    fun personopplysninger(@PathVariable behandlingId: UUID): Ressurs<PersonopplysningerDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(personopplysningerService.hentPersonopplysninger(behandlingId))
    }

    @GetMapping("/fagsak/{fagsakId}")
    fun personopplysningerFraFagsakId(@PathVariable fagsakId: UUID): Ressurs<PersonopplysningerDto> {
        tilgangService.validerTilgangTilFagsak(fagsakId)
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        return Ressurs.success(personopplysningerService.hentPersonopplysninger(aktivIdent))
    }

    @GetMapping("/nav-kontor/behandling/{behandlingId}")
    fun hentNavKontor(@PathVariable behandlingId: UUID): Ressurs<NavKontorEnhet> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val aktivIdent = behandlingService.hentAktivIdent(behandlingId)
        return Ressurs.success(personopplysningerService.hentNavKontor(aktivIdent))
    }

    @PostMapping("/nav-kontor")
    fun hentNavKontorTilFagsak(@RequestBody personIdent: PersonIdentDto): Ressurs<NavKontorEnhet> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdent.personIdent)
        return Ressurs.success(personopplysningerService.hentNavKontor(personIdent.personIdent))
    }

}
