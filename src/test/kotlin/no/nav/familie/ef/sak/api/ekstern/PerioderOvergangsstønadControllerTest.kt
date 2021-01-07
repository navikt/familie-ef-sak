package no.nav.familie.ef.sak.api.ekstern

import io.mockk.verify
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

internal class PerioderOvergangsstønadControllerTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

    @Autowired lateinit var pdlClient: PdlClient

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `verifiser att infotrygdklienten blir kallet på`() {
        val response = restTemplate.exchange<PerioderOvergangsstønadResponse>(
                localhost("/api/ekstern/periode/overgangsstonad"),
                HttpMethod.POST,
                HttpEntity(PerioderOvergangsstønadRequest("01234567890", null, null), headers))
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        // hentIdenter fra pdl legger til en ekstra ident som brukes når vi henter data fra infotrygd
        verify(exactly = 1) {
            val forventetInfotrygdRequest = InfotrygdPerioderOvergangsstønadRequest(identer = setOf("01234567890", "98765432109"),
                                                                                    fomDato = null,
                                                                                    tomDato = null)
            infotrygdReplikaClient.hentPerioderOvergangsstønad(forventetInfotrygdRequest)
        }
        verify(exactly = 1) { pdlClient.hentPersonidenter(any(), true) }
    }
}