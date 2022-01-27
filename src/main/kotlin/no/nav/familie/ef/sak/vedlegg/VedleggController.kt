package no.nav.familie.ef.sak.vedlegg

import no.nav.familie.ef.sak.AuditLoggerEvent
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
@RequestMapping("/api/vedlegg")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedleggController(private val vedleggService: VedleggService,
                        private val tilgangService: TilgangService) {

    @GetMapping("/{behandlingId}")
    fun finnVedleggForBehandling(@PathVariable behandlingId: UUID): Ressurs<JournalposterDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(vedleggService.finnJournalposter(behandlingId))
    }

    @GetMapping("/person/{personIdent}")
    fun finnVedleggForPerson(@PathVariable personIdent: String): Ressurs<List<DokumentinfoDto>> {
        tilgangService.validerTilgangTilPerson(personIdent, AuditLoggerEvent.ACCESS)
        return Ressurs.success(vedleggService.finnDokumentInfo(personIdent))
    }
}
