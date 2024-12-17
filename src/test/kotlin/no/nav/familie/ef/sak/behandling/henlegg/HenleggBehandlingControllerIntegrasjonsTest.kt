package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling.henlegg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID

class HenleggBehandlingControllerIntegrasjonsTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun setUp() {
        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-ef-personhendelse")
        headers.setBearerAuth(lokalTestToken)
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `Skal henlegge behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val respons = henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT, saksbehandlerSignatur = ""))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.data!!.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        assertThat(respons.body?.data!!.henlagtÅrsak).isEqualTo(HenlagtÅrsak.FEILREGISTRERT)
    }

    @Test
    internal fun `Skal henlegge FØRSTEGANGSBEHANDLING`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val respons = henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT, saksbehandlerSignatur = ""))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.data!!.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        assertThat(respons.body?.data!!.henlagtÅrsak).isEqualTo(HenlagtÅrsak.FEILREGISTRERT)
    }

    private fun henleggBehandling(id: UUID): ResponseEntity<Ressurs<BehandlingDto>> =
        restTemplate.exchange(
            localhost("/api/behandling/$id/henlegg"),
            HttpMethod.POST,
            HttpEntity<Ressurs<BehandlingDto>>(headers),
        )

    private fun henlegg(
        id: UUID,
        henlagt: HenlagtDto,
    ): ResponseEntity<Ressurs<BehandlingDto>> =
        restTemplate.exchange<Ressurs<BehandlingDto>>(
            localhost("/api/behandling/$id/henlegg"),
            HttpMethod.POST,
            HttpEntity(henlagt, headers),
        )
}
