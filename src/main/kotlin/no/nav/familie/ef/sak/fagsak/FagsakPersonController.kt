package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.fagsak.dto.FagsakPersonDto
import no.nav.familie.ef.sak.felles.dto.PersonIdentDto
import no.nav.familie.ef.sak.felles.util.FnrUtil.validerIdent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
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
@RequestMapping(path = ["/api/fagsak-person"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakPersonController(
    private val tilgangService: TilgangService,
    private val fagsakPersonService: FagsakPersonService,
    private val fagsakService: FagsakService,
    private val personService: PersonService,
) {
    @GetMapping("{fagsakPersonId}")
    fun hentFagsakPerson(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<FagsakPersonDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val person = fagsakPersonService.hentPerson(fagsakPersonId)
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(person.id)
        return Ressurs.success(
            FagsakPersonDto(
                person.id,
                overgangsstønad = fagsaker.overgangsstønad?.let { fagsakService.fagsakTilDto(it) },
                barnetilsyn = fagsaker.barnetilsyn?.let { fagsakService.fagsakTilDto(it) },
                skolepenger = fagsaker.skolepenger?.let { fagsakService.fagsakTilDto(it) },
            ),
        )
    }

    @PostMapping
    fun hentFagsakPersonIdForPerson(
        @RequestBody personIdentRequest: PersonIdentDto,
    ): Ressurs<UUID> {
        validerIdent(personIdentRequest.personIdent)
        val personIdenter = personService.hentPersonIdenter(personIdentRequest.personIdent)
        tilgangService.validerTilgangTilPersonMedBarn(personIdentRequest.personIdent, AuditLoggerEvent.ACCESS)
        val fagsakPersonId =
            fagsakPersonService
                .hentEllerOpprettPerson(
                    personIdenter.identer(),
                    personIdenter.gjeldende().ident,
                ).id
        return Ressurs.success(
            fagsakPersonId,
        )
    }
}
