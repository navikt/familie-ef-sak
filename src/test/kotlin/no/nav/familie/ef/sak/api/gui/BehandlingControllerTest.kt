package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
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
import java.util.UUID

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
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val respons = hentBehandling(behandling.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        assertThat(respons.body?.data).isNull()
    }

    @Test
    internal fun `Skal annullere behandling`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson("12345678901"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.BLANKETT))
        val respons = annullerBehandling(behandling.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.data!!.resultat).isEqualTo(BehandlingResultat.ANNULLERT)
    }

    private fun hentBehandling(id: UUID): ResponseEntity<Ressurs<BehandlingDto>> {
        return restTemplate.exchange(localhost("/api/behandling/$id"),
                                     HttpMethod.GET,
                                     HttpEntity<Ressurs<BehandlingDto>>(headers))
    }

    private fun annullerBehandling(id: UUID): ResponseEntity<Ressurs<BehandlingDto>> {
        return restTemplate.exchange(localhost("/api/behandling/$id/annuller"),
                                     HttpMethod.POST,
                                     HttpEntity<Ressurs<BehandlingDto>>(headers))
    }
}