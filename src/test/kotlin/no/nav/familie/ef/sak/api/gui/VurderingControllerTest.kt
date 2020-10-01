package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.Testsøknad
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.kontrakter.ef.sak.SakRequest
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("integrasjonstest", "mock-oauth", "mock-integrasjoner", "mock-pdl")
internal class VurderingControllerTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `skal hente inngangsvilkår`() {
        val sak = SakRequest(SøknadMedVedlegg(Testsøknad.søknad, emptyList()), "123", "321")
        val behandlingId = behandlingService.mottaSakOvergangsstønad(sak, emptyMap())

        val respons: ResponseEntity<Ressurs<InngangsvilkårDto>> =
                restTemplate.exchange(localhost("/api/vurdering/$behandlingId/inngangsvilkaar"),
                                      HttpMethod.GET,
                                      HttpEntity<Any>(headers))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body.data).isNotNull
    }
}