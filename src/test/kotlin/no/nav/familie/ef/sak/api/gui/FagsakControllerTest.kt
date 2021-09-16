package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.fagsak.FagsakDto
import no.nav.familie.ef.sak.fagsak.FagsakRequest
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.fagsak.FagsakPerson
import no.nav.familie.ef.sak.fagsak.Stønadstype
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID

internal class FagsakControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal returnere 200 OK med status IKKE_TILGANG dersom man ikke har tilgang til brukeren`() {
        val respons: ResponseEntity<Ressurs<FagsakDto>> = hentFagsakForPerson()

        Assertions.assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        Assertions.assertThat(respons.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        Assertions.assertThat(respons.body?.data).isNull()
    }

    private fun hentFagsakForPerson(): ResponseEntity<Ressurs<FagsakDto>> {
        val fagsakRequest = FagsakRequest("ikkeTilgang", Stønadstype.OVERGANGSSTØNAD)

        return restTemplate.exchange(localhost("/api/fagsak"),
                                     HttpMethod.POST,
                                     HttpEntity(fagsakRequest, headers))
    }

    @Test
    internal fun `Gitt fagsak med behandlinger finnes når get fagsak endpoint kalles skal det returneres 200 OK med fagsakDto`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson("01010199999"))))
        behandlingRepository.insert(behandling(fagsak, aktiv = false))
        behandlingRepository.insert(behandling(fagsak, aktiv = false))

        val fagsakForId = hentFagsakForId(fagsak.id)
        Assertions.assertThat(fagsakForId.data?.id).isEqualTo(fagsak.id)
        Assertions.assertThat(fagsakForId.data?.behandlinger?.size).isEqualTo(2)
        Assertions.assertThat(fagsakForId.data?.behandlinger!!.all { it.resultat == BehandlingResultat.IKKE_SATT })
    }

    private fun hentFagsakForId(fagsakId: UUID): Ressurs<FagsakDto> {
        val response = restTemplate.exchange<Ressurs<FagsakDto>>(localhost("/api/fagsak/$fagsakId"),
                                                                 HttpMethod.GET,
                                                                 HttpEntity<Any>(headers))

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body.status).isEqualTo(Ressurs.Status.SUKSESS)
        return response.body
    }
}