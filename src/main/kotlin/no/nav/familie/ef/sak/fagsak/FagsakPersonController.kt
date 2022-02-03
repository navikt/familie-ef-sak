package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.fagsak.dto.FagsakPersonDto
import no.nav.familie.ef.sak.fagsak.dto.FagsakPersonUtvidetDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
        private val fagsakService: FagsakService
) {

    @GetMapping("{fagsakPersonId}")
    fun hentFagsakPerson(@PathVariable fagsakPersonId: UUID): Ressurs<FagsakPersonDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val person = fagsakPersonService.hentPerson(fagsakPersonId)
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(person.id)
        return Ressurs.success(FagsakPersonDto(
                person.id,
                overgangsstønad = fagsaker.overgangsstønad?.id,
                barnetilsyn = fagsaker.barnetilsyn?.id,
                skolepenger = fagsaker.skolepenger?.id
        ))
    }

    @GetMapping("{fagsakPersonId}/utvidet")
    fun hentFagsakPersonUtvidet(@PathVariable fagsakPersonId: UUID): Ressurs<FagsakPersonUtvidetDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val person = fagsakPersonService.hentPerson(fagsakPersonId)
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(person.id)
        return Ressurs.success(FagsakPersonUtvidetDto(
                person.id,
                overgangsstønad = fagsaker.overgangsstønad?.let { fagsakService.fagsakTilDto(it) },
                barnetilsyn = fagsaker.barnetilsyn?.let { fagsakService.fagsakTilDto(it) },
                skolepenger = fagsaker.skolepenger?.let { fagsakService.fagsakTilDto(it) }
        ))
    }


}