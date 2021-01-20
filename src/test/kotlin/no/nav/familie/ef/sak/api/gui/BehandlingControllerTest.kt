package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.BehandlingDto
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
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
import java.util.*

internal class BehandlingControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal returnere 200 OK med status IKKE_TILGANG dersom man ikke har tilgang til brukeren`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson("ikkeTilgang"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, aktiv = false))
        val respons = hentBehandling(behandling.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        assertThat(respons.body?.data).isNull()
    }

    private fun hentBehandling(id: UUID): ResponseEntity<Ressurs<BehandlingDto>> {
        return restTemplate.exchange(localhost("/api/behandling/$id"),
                                     HttpMethod.GET,
                                     HttpEntity<Ressurs<BehandlingDto>>(headers))
    }
}