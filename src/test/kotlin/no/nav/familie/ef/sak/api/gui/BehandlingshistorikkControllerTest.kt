package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.dto.BehandlingshistorikkDto
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.felles.domain.JsonWrapper
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingshistorikkControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal returnere 200 OK med status IKKE_TILGANG dersom man ikke har tilgang til brukeren`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson("ikkeTilgang"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, aktiv = false))
        val respons = hentHistorikk(behandling.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        assertThat(respons.body?.data).isNull()
    }

    @Test
    internal fun `skal returnere sortert historikk, sist opprettet først`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak, aktiv = false))

        leggInnHistorikk(behandling, "1", LocalDateTime.now())
        leggInnHistorikk(behandling, "2", LocalDateTime.now().minusDays(1))
        leggInnHistorikk(behandling, "3", LocalDateTime.now().plusDays(1))

        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body.data!!.map { it.endretAv }).containsExactly("3", "1", "2")
    }

    @Test
    internal fun `skal returnere metadata som json`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak, aktiv = false))

        val jsonMap = mapOf("key" to "value")
        val metadata = JsonWrapper(objectMapper.writeValueAsString(jsonMap))
        behandlingshistorikkRepository.insert(Behandlingshistorikk(behandlingId = behandling.id,
                                                                   steg = behandling.steg,
                                                                   metadata = metadata))

        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body.data!!.first().metadata).isEqualTo(jsonMap)
    }

    private fun leggInnHistorikk(behandling: Behandling, opprettetAv: String, endretTid: LocalDateTime) {
        behandlingshistorikkRepository.insert(Behandlingshistorikk(behandlingId = behandling.id,
                                                                   steg = behandling.steg,
                                                                   opprettetAv = opprettetAv,
                                                                   endretTid = endretTid))
    }

    private fun hentHistorikk(id: UUID): ResponseEntity<Ressurs<List<BehandlingshistorikkDto>>> {
        return restTemplate.exchange(localhost("/api/behandlingshistorikk/$id"),
                                     HttpMethod.GET,
                                     HttpEntity<Ressurs<List<BehandlingshistorikkDto>>>(headers))
    }
}