package no.nav.familie.ef.sak.api.gui

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.VedtaksbrevService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/brev"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtaksbrevController(private val brevService: VedtaksbrevService,
                            private val tilgangService: TilgangService) {

    @GetMapping("/{behandlingId}")
    fun hentBrev(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(brevService.hentBrev(behandlingId))
    }

    @PostMapping("/{behandlingId}/{brevMal}")
    fun lagSaksbehandlerbrev(@PathVariable behandlingId: UUID,
                             @PathVariable brevMal: String,
                             @RequestBody brevRequest: JsonNode): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(brevService.lagSaksbehandlerBrev(behandlingId, brevRequest, brevMal))
    }

    @PostMapping("/{behandlingId}")
    fun lagBeslutterbrev(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        tilgangService.validerHarBeslutterrolle()
        return Ressurs.success(brevService.lagBeslutterBrev(behandlingId))
    }

    @GetMapping("/{behandlingId}/tilkjent-ytelse")
    fun hentTilkjenteAndeler(@PathVariable behandlingId: UUID): Ressurs<TilkjentYtelseDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)

        val andel1 = Andel(1000, LocalDate.now().minusMonths(4), LocalDate.now().minusMonths(2), 500)
        val andel2 = Andel(2001, LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1), 200)
        val andel3 = Andel(3000, LocalDate.now().minusMonths(1), LocalDate.now(), 100)
        val tilkjentYtelseDto = TilkjentYtelseDto(andeler = listOf(andel1, andel2, andel3))
        return Ressurs.success(tilkjentYtelseDto)
    }


}

data class TilkjentYtelseDto(val andeler: List<Andel>)
data class Andel(val beløp: Int,
                 val fraDato: LocalDate,
                 val tilDato: LocalDate,
                 val inntekt: Int)
