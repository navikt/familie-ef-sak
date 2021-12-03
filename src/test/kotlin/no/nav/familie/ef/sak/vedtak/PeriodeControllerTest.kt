package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
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

internal class PeriodeControllerTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `kan kalle på andelshistorikk uten query param`() {
        val fagsak = fagsakRepository.insert(fagsak(fagsakpersoner(setOf("1"))))
        behandlingRepository.insert(behandling(fagsak))

        val response: ResponseEntity<Ressurs<List<AndelHistorikkDto>>> =
                restTemplate.exchange(localhost("/api/perioder/fagsak/${fagsak.id}/historikk"),
                                      HttpMethod.GET,
                                      HttpEntity(null, headers))
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
    }

    @Test
    internal fun `kan kalle på endepunkt med query param`() {
        val fagsak = fagsakRepository.insert(fagsak(fagsakpersoner(setOf("1"))))
        behandlingRepository.insert(behandling(fagsak))

        val response: ResponseEntity<Ressurs<List<AndelHistorikkDto>>> =
                restTemplate.exchange(localhost("/api/perioder/fagsak/${fagsak.id}/historikk?tomBehandlingId=${fagsak.id}"),
                                      HttpMethod.GET,
                                      HttpEntity(null, headers))
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
    }
}