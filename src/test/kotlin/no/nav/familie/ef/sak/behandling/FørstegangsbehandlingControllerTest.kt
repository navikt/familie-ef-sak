package no.nav.familie.ef.sak.behandling

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.FørstegangsbehandlingDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider
import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.exchange
import java.time.LocalDate
import java.util.UUID

internal class FørstegangsbehandlingControllerTest : OppslagSpringRunnerTest() {
    private val fagsakUtenBehandling = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD)
    private val fagsakMedBehandling = fagsak(stønadstype = StønadType.SKOLEPENGER)
    private val fagsakMedHenlagtBehandling = fagsak(stønadstype = StønadType.BARNETILSYN)
    private val eksisterendeBehandling =
        behandling(
            fagsakMedBehandling,
            status = BehandlingStatus.FERDIGSTILT,
            steg = StegType.BEHANDLING_FERDIGSTILT,
            resultat = BehandlingResultat.INNVILGET,
        )
    private val henlagtBehandling =
        behandling(
            fagsakMedBehandling,
            status = BehandlingStatus.FERDIGSTILT,
            steg = StegType.BEHANDLING_FERDIGSTILT,
            resultat = BehandlingResultat.HENLAGT,
        )

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        testoppsettService.lagreFagsak(fagsakUtenBehandling)
        testoppsettService.lagreFagsak(fagsakMedBehandling)
        testoppsettService.lagreFagsak(fagsakMedHenlagtBehandling)
        behandlingRepository.insert(eksisterendeBehandling)
        behandlingRepository.insert(henlagtBehandling)
        every { infotrygdReplikaClient.hentSammenslåttePerioder(any()) } returns
            InfotrygdPeriodeResponse(
                emptyList(),
                emptyList(),
                emptyList(),
            )
    }

    @AfterEach
    internal fun tearDown() {
        InfotrygdReplikaMock.resetMock(infotrygdReplikaClient)
    }

    @Test
    internal fun `skal kunne opprette førstegangsbehandling`() {
        opprettBehandling(
            fagsakUtenBehandling.id,
            FørstegangsbehandlingDto(
                BehandlingÅrsak.PAPIRSØKNAD,
                LocalDate.now(),
                listOf(
                    BarnSomSkalFødes(LocalDate.now().plusDays(100)),
                ),
            ),
        )
    }

    @Test
    internal fun `skal kunne opprette førstegangsbehandling dersom det eksisterer en som er henlagt fra før`() {
        opprettBehandling(
            fagsakMedHenlagtBehandling.id,
            FørstegangsbehandlingDto(
                BehandlingÅrsak.PAPIRSØKNAD,
                LocalDate.now(),
                emptyList(),
            ),
        )
    }

    @Test
    internal fun `skal ikke kunne opprette førstegangsbehandling hvis det allerede finnes en behandling`() {
        opprettBehandling(
            fagsakMedBehandling.id,
            FørstegangsbehandlingDto(
                BehandlingÅrsak.PAPIRSØKNAD,
                LocalDate.now(),
            ),
        ) { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.frontendFeilmelding).contains("Kan ikke opprette en førstegangsbehandling når siste behandling")
        }
    }

    private fun opprettBehandling(
        fagsakId: UUID,
        førstegangsbehandlingRequest: FørstegangsbehandlingDto,
        validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseOK(),
    ) {
        val response =
            testRestTemplate.exchange<Ressurs<UUID>>(
                localhost("/api/forstegangsbehandling/$fagsakId/opprett"),
                HttpMethod.POST,
                HttpEntity(førstegangsbehandlingRequest, headers),
            )
        validator.invoke(response)
    }

    private fun <T> responseOK(): (ResponseEntity<Ressurs<T>>) -> Unit =
        {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        }
}
