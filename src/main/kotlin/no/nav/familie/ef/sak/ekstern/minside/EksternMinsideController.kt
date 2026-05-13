package no.nav.familie.ef.sak.ekstern.minside

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.sikkerhet.EksternBrukerUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/minside"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class EksternMinsideController(
    private val eksternMinsideService: EksternMinsideService,
) {
    /**
     * familie-ef-soknad-api bruker denne på vegne av "minside"
     */
    @GetMapping("stonadsperioder")
    fun finnStønadsperioderForBruker(): Ressurs<MineStønaderDto> {
        sjekkAcrLevel4()
        feilHvisIkke(SikkerhetContext.kallKommerFraFamilieEfSøknadApi(), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return Ressurs.success(eksternMinsideService.hentStønadsperioderForBruker(EksternBrukerUtils.hentFnrFraToken()))
    }

    private fun sjekkAcrLevel4() {
        val authentication =
            SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
                ?: error("Mangler autentisering")
        val acr = authentication.token.getClaimAsString("acr")
        feilHvisIkke(acr == "Level4", HttpStatus.UNAUTHORIZED) {
            "Påloggingsnivå er ikke høyt nok. Krever Level4, fikk $acr"
        }
    }
}
