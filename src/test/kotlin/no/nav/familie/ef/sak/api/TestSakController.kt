package no.nav.familie.ef.sak.no.nav.familie.ef.sak.api

import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.kontrakter.ef.sak.SakRequest
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/external/sak/"])
@Unprotected
@Validated
class TestSakController(private val behandlingService: BehandlingService) {

    @PostMapping("dummy")
    fun dummy(): UUID {
        val sak = SakRequest(SøknadMedVedlegg(Testsøknad.søknadOvergangsstønad, emptyList()), "123", "321")
        return behandlingService.mottaSakOvergangsstønad(sak, emptyMap())
    }

}