package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.behandling.MigreringService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
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
class MigreringController(private val migreringService: MigreringService,
                          private val tilgangService: TilgangService) {

    @GetMapping("{fagsakId}")
    fun hentBehandling(@PathVariable fagsakId: UUID): Ressurs<MigreringInfo> {
        tilgangService.validerTilgangTilFagsak(fagsakId)
        return Ressurs.success(migreringService.hentMigreringInfo(fagsakId))
    }

    @PostMapping("{fagsakId}")
    fun resetSteg(@PathVariable fagsakId: UUID): Ressurs<String> {
        tilgangService.validerTilgangTilFagsak(fagsakId)
        migreringService.migrerFagsak(fagsakId)
        return Ressurs.success("OK")
    }
}
