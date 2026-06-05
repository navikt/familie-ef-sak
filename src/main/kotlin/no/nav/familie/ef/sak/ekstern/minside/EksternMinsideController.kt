package no.nav.familie.ef.sak.ekstern.minside

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.sikkerhet.EksternBrukerUtils
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
class EksternMinsideController(
    private val eksternMinsideService: EksternMinsideService,
) {
    /**
     * familie-ef-soknad-api bruker denne på vegne av "minside"
     */
    @GetMapping("stonadsperioder")
    fun finnStønadsperioderForBruker(): Ressurs<MineStønaderDto> {
        SikkerhetContext.sjekkAcrLevel4()
        feilHvisIkke(SikkerhetContext.kallKommerFraFamilieEfSøknadApi(), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return Ressurs.success(eksternMinsideService.hentStønadsperioderForBruker(EksternBrukerUtils.hentFnrFraToken()))
    }
}
