package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class PeriodeControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `kan kalle på andelshistorikk uten query param`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("1"))))
        behandlingRepository.insert(behandling(fagsak))

        val response: ResponseEntity<Ressurs<List<AndelHistorikkDto>>> =
            restTemplate.exchange(
                localhost("/api/perioder/fagsak/${fagsak.id}/historikk"),
                HttpMethod.GET,
                HttpEntity(null, headers),
                object : ParameterizedTypeReference<Ressurs<List<AndelHistorikkDto>>>() {},
            )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
    }

    @Test
    internal fun `kan kalle på endepunkt med query param`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("1"))))
        behandlingRepository.insert(behandling(fagsak))

        val response: ResponseEntity<Ressurs<List<AndelHistorikkDto>>> =
            restTemplate.exchange(
                localhost("/api/perioder/fagsak/${fagsak.id}/historikk?tilOgMedBehandlingId=${fagsak.id}"),
                HttpMethod.GET,
                HttpEntity(null, headers),
            )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
    }
}
