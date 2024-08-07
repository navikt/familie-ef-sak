package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.migrering.MigreringService
import no.nav.familie.ef.sak.fagsak.dto.MigrerRequestDto
import no.nav.familie.ef.sak.fagsak.dto.MigreringInfo
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
@RequestMapping(path = ["/api/migrering"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class MigreringController(
    private val migreringService: MigreringService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("{fagsakPersonId}")
    fun hentMigreringInfo(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<MigreringInfo> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(migreringService.hentMigreringInfo(fagsakPersonId))
    }

    @PostMapping("{fagsakPersonId}")
    fun migrerOvergangsstønad(
        @PathVariable fagsakPersonId: UUID,
        @RequestBody request: MigrerRequestDto?,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.CREATE)
        return Ressurs.success(
            migreringService.migrerOvergangsstønad(
                fagsakPersonId,
                request ?: MigrerRequestDto(),
            ),
        )
    }

    @PostMapping("{fagsakPersonId}/barnetilsyn")
    fun migrerBarnetilsyn(
        @PathVariable fagsakPersonId: UUID,
        @RequestBody request: MigrerRequestDto?,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.CREATE)
        return Ressurs.success(migreringService.migrerBarnetilsyn(fagsakPersonId, request ?: MigrerRequestDto()))
    }
}
