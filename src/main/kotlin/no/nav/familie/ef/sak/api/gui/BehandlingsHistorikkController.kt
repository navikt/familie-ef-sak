package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.BehandlingsHistorikkDto
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.mapper.BehandlingHistorikkMapper
import no.nav.familie.ef.sak.repository.domain.BehandlingsHistorikk
import no.nav.familie.ef.sak.service.BehandlingHistorikkService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/behandlinghistorikk"])
@ProtectedWithClaims(issuer = "azuread")
class BehandlingsHistorikkController(private val behandlingHistorikkService: BehandlingHistorikkService) {

    @GetMapping("{historikkid}")
    fun hentTilkjentYtelse(@PathVariable historikkid: UUID): ResponseEntity<List<BehandlingsHistorikkDto>> {
        return ResponseEntity.ok(BehandlingHistorikkMapper.transform(behandlingHistorikkService.finnBehandling (historikkid)))
    }

}