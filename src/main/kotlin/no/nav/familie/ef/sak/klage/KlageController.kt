package no.nav.familie.ef.sak.klage

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.klage.dto.KlagebehandlingerDto
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.ef.sak.klage.dto.ÅpneKlagerInfotrygdDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/klage"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class KlageController(
    private val tilgangService: TilgangService,
    private val klageService: KlageService,
    private val fagsakService: FagsakService,
) {
    @PostMapping("/fagsak/{fagsakId}")
    fun opprettKlage(
        @PathVariable fagsakId: UUID,
        @RequestBody opprettKlageDto: OpprettKlageDto,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        klageService.validerOgOpprettKlage(fagsakService.hentFagsak(fagsakId), opprettKlageDto)
        return Ressurs.success(fagsakId)
    }

    @GetMapping("/fagsak-person/{fagsakPersonId}")
    fun hentKlagebehandlinger(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<KlagebehandlingerDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(klageService.hentBehandlinger(fagsakPersonId))
    }

    @GetMapping("/fagsak-person/{fagsakPersonId}/infotrygd")
    fun hentInfotrygdStatus(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<ÅpneKlagerInfotrygdDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(klageService.hentÅpneKlagerInfotrygd(fagsakPersonId))
    }
}
