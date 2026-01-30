package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.FagsakRequest
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.fagsak.dto.FagsakDto
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID

internal class FagsakControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal returnere 200 OK med status IKKE_TILGANG dersom man ikke har tilgang til brukeren`() {
        val respons: ResponseEntity<Ressurs<FagsakDto>> = hentFagsakForPerson()

        assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        assertThat(respons.body?.data).isNull()
    }

    @Test
    internal fun `Gitt fagsak med behandlinger finnes når get fagsak endpoint kalles skal det returneres 200 OK med fagsakDto`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("01010199999"))))
        behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        behandlingRepository.insert(behandling(fagsak))

        val fagsakForId = hentFagsakForId(fagsak.id)
        assertThat(fagsakForId?.data?.id).isEqualTo(fagsak.id)
        assertThat(fagsakForId?.data?.behandlinger?.size).isEqualTo(2)
        assertThat(fagsakForId?.data?.behandlinger!!.all { it.resultat == BehandlingResultat.IKKE_SATT })
    }

    @Test
    internal fun `Skal returnere fagsak på behandlingId `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("01010199999"))))
        val behandling = behandling(fagsak)
        behandlingRepository.insert(behandling)

        val hentetFagsak = hentFagsakPåBehandlingId(behandling.id)
        assertThat(hentetFagsak?.data?.id).isEqualTo(fagsak.id)
        assertThat(hentetFagsak?.data?.behandlinger?.size).isEqualTo(1)
        assertThat(
            hentetFagsak
                ?.data
                ?.behandlinger
                ?.first()
                ?.id,
        ).isEqualTo(behandling.id)
    }

    private fun hentFagsakForPerson(): ResponseEntity<Ressurs<FagsakDto>> {
        val fagsakRequest = FagsakRequest("ikkeTilgang", StønadType.OVERGANGSSTØNAD)

        return testRestTemplate.exchange(
            localhost("/api/fagsak"),
            HttpMethod.POST,
            HttpEntity(fagsakRequest, headers),
        )
    }

    private fun hentFagsakForId(fagsakId: UUID): Ressurs<FagsakDto>? {
        val response =
            testRestTemplate.exchange<Ressurs<FagsakDto>>(
                localhost("/api/fagsak/$fagsakId"),
                HttpMethod.GET,
                HttpEntity<Any>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        return response.body
    }

    private fun hentFagsakPåBehandlingId(behandlingId: UUID): Ressurs<FagsakDto>? {
        val response =
            testRestTemplate.exchange<Ressurs<FagsakDto>>(
                localhost("/api/fagsak/behandling/$behandlingId"),
                HttpMethod.GET,
                HttpEntity<Any>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        return response.body
    }
}
