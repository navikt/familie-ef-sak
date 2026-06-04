package no.nav.familie.ef.sak.utestengelse

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/utestengelse")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class UtestengelseController(
    private val tilgangService: TilgangService,
    private val utestengelseService: UtestengelseService,
    private val fagsakService: FagsakService,
) {
    @GetMapping("/{fagsakPersonId}")
    fun hentUtestengelser(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<List<UtestengelseDto>> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(utestengelseService.hentUtestengelser(fagsakPersonId).map { it.tilDto() })
    }

    @GetMapping("/behandling/{behandlingId}")
    fun hentUtestengelserForBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<List<UtestengelseDto>> {
        val fagsakPersonId = fagsakService.hentFagsakForBehandling(behandlingId).fagsakPersonId
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(utestengelseService.hentUtestengelser(fagsakPersonId).map { it.tilDto() })
    }

    @PostMapping("/{fagsakPersonId}")
    fun opprettUtestengelser(
        @PathVariable fagsakPersonId: UUID,
        @RequestBody opprettUtestengelseDto: OpprettUtestengelseDto,
    ): Ressurs<UtestengelseDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.CREATE)
        feilHvisIkke(fagsakPersonId == opprettUtestengelseDto.fagsakPersonId) {
            "Innsendt fagsakPersonId matcher ikke body(${opprettUtestengelseDto.fagsakPersonId})"
        }
        return Ressurs.success(utestengelseService.opprettUtestengelse(opprettUtestengelseDto))
    }

    @DeleteMapping("/{fagsakPersonId}/{id}")
    fun slettUtestengelse(
        @PathVariable fagsakPersonId: UUID,
        @PathVariable id: UUID,
    ): Ressurs<String> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.DELETE)
        utestengelseService.slettUtestengelse(fagsakPersonId, id)
        return Ressurs.success("OK")
    }
}
