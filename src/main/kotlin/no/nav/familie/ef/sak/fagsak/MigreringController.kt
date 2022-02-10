package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.MigreringService
import no.nav.familie.ef.sak.fagsak.dto.MigreringInfo
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/migrering"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class MigreringController(private val migreringService: MigreringService,
                          private val tilgangService: TilgangService) {

    @GetMapping("{fagsakPersonId}")
    fun hentMigreringInfo(@PathVariable fagsakPersonId: UUID): Ressurs<MigreringInfo> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(migreringService.hentMigreringInfo(fagsakPersonId))
    }

    @PostMapping("{fagsakPersonId}")
    fun migrerFagsak(@PathVariable fagsakId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakId, AuditLoggerEvent.CREATE)
        return Ressurs.success(migreringService.migrerFagsak(fagsakId))
    }
}
