package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto
import no.nav.familie.kontrakter.ef.personhendelse.NyeBarnDto
import no.nav.familie.kontrakter.felles.PersonIdent
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
@RequestMapping(path = ["/api/behandling/barn"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BarnController(
    private val nyeBarnService: NyeBarnService,
    private val tilgangService: TilgangService
) {

    @PostMapping("nye-eller-tidligere-fodte-barn")
    // denne skal kalles på fra ef-personhendelse(client_credential) for å opprette oppgaver for nye eller for tidligt fødte barn
    fun finnNyeEllerTidligereFødteBarn(@RequestBody personIdent: PersonIdent): Ressurs<NyeBarnDto> {
        if (!SikkerhetContext.erMaskinTilMaskinToken()) {
            tilgangService.validerTilgangTilPerson(personIdent.ident, AuditLoggerEvent.ACCESS)
        }
        return Ressurs.success(nyeBarnService.finnNyeEllerTidligereFødteBarn(personIdent))
    }

    @GetMapping("fagsak/{fagsakId}/nye-barn")
    fun finnNyeBarnSidenGjeldendeBehandlingForFagsak(
        @PathVariable("fagsakId")
        fagsakId: UUID
    ): Ressurs<List<BarnMinimumDto>> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(fagsakId))
    }
}
