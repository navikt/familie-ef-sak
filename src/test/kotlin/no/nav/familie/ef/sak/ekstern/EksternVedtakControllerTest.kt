package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.util.medContentTypeJsonUTF8
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity

internal class EksternVedtakControllerTest : OppslagSpringRunnerTest() {
    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @AfterEach
    fun tearDown() {
    }

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `hentVedtak skal returnere behandlinger fra ef-sak og tilbakekreving`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("123"))))
        val førstegangsbehandling =
            behandling(
                fagsak = fagsak,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                status = BehandlingStatus.FERDIGSTILT,
                eksternId = 1,
                resultat = BehandlingResultat.INNVILGET,
            )

        behandlingRepository.insertAll(listOf(førstegangsbehandling))

        val vedtakResponse = hentVedtak(fagsak.eksternId).body?.data!!

        assertThat(vedtakResponse).hasSize(2)
        assertThat(vedtakResponse.map { it.fagsystemType })
            .containsExactlyInAnyOrder(FagsystemType.ORDNIÆR, FagsystemType.TILBAKEKREVING)
    }

    private fun hentVedtak(eksternFagsakId: Long): ResponseEntity<Ressurs<List<FagsystemVedtak>>> =
        restTemplate.exchange(
            localhost("/api/ekstern/vedtak/$eksternFagsakId"),
            HttpMethod.GET,
            HttpEntity(null, headers.medContentTypeJsonUTF8()),
        )
}
