package no.nav.familie.ef.sak.api.soknad

import no.nav.familie.ef.sak.api.dto.SøknadDatoerDto
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/soknad"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SøknadController(private val søknadService: SøknadService,
                       private val tilgangService: TilgangService) {

    @GetMapping("/{behandlingId}/datoer")
    fun hentSøknadDatoer(@PathVariable behandlingId: UUID): Ressurs<SøknadDatoerDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val overgangsstønad = søknadService.hentOvergangsstønad(behandlingId)
        return Ressurs.success(SøknadDatoerDto(søknadsdato = overgangsstønad.datoMottatt,
                                               søkerStønadFra = overgangsstønad.søkerFra))
    }
}