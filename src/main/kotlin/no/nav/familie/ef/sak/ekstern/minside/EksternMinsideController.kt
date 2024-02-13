package no.nav.familie.ef.sak.ekstern.minside

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/minside"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
@ProtectedWithClaims(issuer = EksternBrukerUtils.ISSUER_TOKENX, claimMap = ["acr=Level4"])
class EksternMinsideController(
    private val eksternMinsideService: EksternMinsideService,
) {
    /**
     * familie-ef-soknad-api bruker denne på vegne av "minside"
     */
    @GetMapping("stonadsperioder")
    fun finnStønadsperioderForBruker(): Ressurs<MineStønaderDto> {
        feilHvisIkke(SikkerhetContext.kallKommerFraFamilieEfSøknadApi(), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return Ressurs.success(eksternMinsideService.hentStønadsperioderForBruker(EksternBrukerUtils.hentFnrFraToken()))
    }
}
