package no.nav.familie.ef.sak.ekstern.soknad

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
    path = ["/api/ekstern/soknad"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class EksternSøknadController(
    private val eksternSøknadService: EksternSøknadService,
) {
    @GetMapping("har-vedtak-pa-gammelt-regelverk")
    fun harTidligereInnvilgetVedtak(): Ressurs<TidligereVedtakStatus> {
        SikkerhetContext.sjekkAcrLevel4()
        feilHvisIkke(SikkerhetContext.kallKommerFraFamilieEfSøknadApi(), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return Ressurs.success(eksternSøknadService.harTidligereInnvilgetVedtak(EksternBrukerUtils.hentFnrFraToken()))
    }
}
