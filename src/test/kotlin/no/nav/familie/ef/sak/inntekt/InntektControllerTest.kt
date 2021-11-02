package no.nav.familie.ef.sak.inntekt

import io.mockk.verify
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.inntekt.ekstern.InntektClient
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import java.time.YearMonth
import java.util.UUID

internal class InntektControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var inntektClient: InntektClient

    private val fagsak = fagsak(identer = fagsakpersoner(setOf("1")))

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        fagsakRepository.insert(fagsak)
    }

    @Test
    internal fun `skal hente inntekt med fom og tom datoer som sendes med query param`() {
        val inntekt = hentInntekt(fagsak.id)
        assertThat(inntekt.body!!.getDataOrThrow()).isNotNull
        verify { inntektClient.hentInntekt(any(), YearMonth.of(2021, 1), YearMonth.of(2021, 2)) }
    }

    private fun hentInntekt(fagsakId: UUID): ResponseEntity<Ressurs<InntektResponseDto>> =
            restTemplate.exchange(localhost("/api/inntekt/fagsak/$fagsakId?fom=2021-01&tom=2021-02"),
                                  HttpMethod.GET,
                                  HttpEntity<Ressurs<InntektResponseDto>>(headers))
}