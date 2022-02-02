package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPersonOld
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

    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal returnere 200 OK med status IKKE_TILGANG dersom man ikke har tilgang til brukeren`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(FagsakPersonOld("ikkeTilgang"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val respons = hentBehandling(behandling.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        assertThat(respons.body?.data).isNull()
    }

    @Test
    internal fun `Skal henlegge behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(FagsakPersonOld("12345678901"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.BLANKETT))
        val respons = henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.BEHANDLES_I_GOSYS))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.data!!.resultat).isEqualTo(BehandlingResultat.HENLAGT)
    }

    @Test
    internal fun `Skal ikke være mulig å henlegge blankett med annet enn BEHANDLES_I_GOSYS`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(FagsakPersonOld("12345678901"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.BLANKETT))
        val respons = henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(respons.body?.frontendFeilmelding).isEqualTo("Blankett kan bare henlegges med årsak BEHANDLES_I_GOSYS")
    }

    @Test
    internal fun `Skal henlegge FØRSTEGANGSBEHANDLING`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(FagsakPersonOld("12345678901"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val respons = henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.data!!.resultat).isEqualTo(BehandlingResultat.HENLAGT)
    }

    private fun hentBehandling(id: UUID): ResponseEntity<Ressurs<BehandlingDto>> {
        return restTemplate.exchange(localhost("/api/behandling/$id"),
                                     HttpMethod.GET,
                                     HttpEntity<Ressurs<BehandlingDto>>(headers))
    }

    private fun henleggBehandling(id: UUID): ResponseEntity<Ressurs<BehandlingDto>> {
        return restTemplate.exchange(localhost("/api/behandling/$id/henlegg"),
                                     HttpMethod.POST,
                                     HttpEntity<Ressurs<BehandlingDto>>(headers))
    }


    private fun henlegg(id: UUID, henlagt: HenlagtDto): ResponseEntity<Ressurs<BehandlingDto>> {
        return restTemplate.exchange<Ressurs<BehandlingDto>>(localhost("/api/behandling/$id/henlegg"),
                                                             HttpMethod.POST,
                                                             HttpEntity(henlagt, headers))
    }
}