package no.nav.familie.ef.sak.no.nav.familie.ef.sak.api

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.Testsøknad
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.kontrakter.ef.sak.SakRequest
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/external/sak/"])
@Unprotected
@Validated
class TestSakController(private val behandlingService: BehandlingService) {

    @PostMapping("dummy")
    fun dummy(): HttpStatus {
        val sak = SakRequest(SøknadMedVedlegg(Testsøknad.søknad, emptyList()), "123", "321")
        behandlingService.mottaSakOvergangsstønad(sak, emptyMap())

        return HttpStatus.CREATED
    }

}