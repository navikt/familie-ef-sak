package no.nav.familie.ef.sak.fagsak

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.fagsak.dto.MigrerRequestDto
import no.nav.familie.ef.sak.fagsak.dto.MigreringInfo
import no.nav.familie.ef.sak.felles.util.DatoUtil
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
import no.nav.familie.ef.sak.infrastruktur.config.IverksettClientMock
import no.nav.familie.ef.sak.infrastruktur.config.IverksettClientMock.Companion.mockSimulering
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class MigreringControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var iverksettClient: IverksettClient

    @Autowired
    private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @BeforeEach
    fun setUp() {
        mockkObject(DatoUtil)
        every { DatoUtil.årMånedNå() } returns YearMonth.of(2023, 5)
        every { DatoUtil.dagensDato() } returns LocalDate.of(2023, 5, 7)
        mockSimulering(iverksettClient)
        mockInfotrygdPerioder()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(DatoUtil)
        IverksettClientMock.clearMock(iverksettClient)
        InfotrygdReplikaMock.resetMock(infotrygdReplikaClient)
    }

    @ParameterizedTest
    @ValueSource(strings = [OVERGANGSSTØNAD_STI, BARNETILSYN_STI])
    fun `veileder med persontilgang skal få 403 og migrering skal ikke starte`(sti: String) {
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", StønadType.OVERGANGSSTØNAD)
        headers.setBearerAuth(lokalVeilederToken)

        val tasksFørKall = taskService.findAll().map { it.id }

        val response = migrer(fagsak.fagsakPersonId, sti)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        verifiserAtMigreringIkkeErStartet(fagsak.id, tasksFørKall)
    }

    @ParameterizedTest
    @ValueSource(strings = [OVERGANGSSTØNAD_STI, BARNETILSYN_STI])
    fun `maskintoken skal få 403 og migrering skal ikke starte`(sti: String) {
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", StønadType.OVERGANGSSTØNAD)
        headers.setBearerAuth(clientToken())

        val tasksFørKall = taskService.findAll().map { it.id }

        val response = migrer(fagsak.fagsakPersonId, sti)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        verifiserAtMigreringIkkeErStartet(fagsak.id, tasksFørKall)
    }

    @ParameterizedTest
    @ValueSource(strings = [OVERGANGSSTØNAD_STI, BARNETILSYN_STI])
    fun `saksbehandler uten persontilgang skal få 403 og migrering skal ikke starte`(sti: String) {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("ikkeTilgang"))))
        headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.saksbehandlerRolle))

        val tasksFørKall = taskService.findAll().map { it.id }

        val response = migrer(fagsak.fagsakPersonId, sti)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        verifiserAtMigreringIkkeErStartet(fagsak.id, tasksFørKall)
    }

    @Test
    fun `saksbehandler med persontilgang skal kunne migrere overgangsstønad`() {
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", StønadType.OVERGANGSSTØNAD)
        headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.saksbehandlerRolle))

        val response = migrer(fagsak.fagsakPersonId, OVERGANGSSTØNAD_STI)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).map { it.id }).containsExactly(response.body?.data)
        verify(exactly = 1) { iverksettClient.iverksettUtenBrev(any()) }
    }

    @Test
    fun `saksbehandler med persontilgang skal kunne migrere barnetilsyn`() {
        mockInfotrygdPerioder(utgifterBarnetilsyn = 100)
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", StønadType.BARNETILSYN)
        headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.saksbehandlerRolle))

        val response = migrer(fagsak.fagsakPersonId, BARNETILSYN_STI)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).map { it.id }).containsExactly(response.body?.data)
        verify(exactly = 1) { iverksettClient.iverksettUtenBrev(any()) }
    }

    @Test
    fun `veileder skal fortsatt kunne hente migreringsinfo`() {
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", StønadType.OVERGANGSSTØNAD)
        headers.setBearerAuth(lokalVeilederToken)

        val response =
            testRestTemplate.exchange<Ressurs<MigreringInfo>>(
                localhost("/api/migrering/${fagsak.fagsakPersonId}"),
                HttpMethod.GET,
                HttpEntity<Void>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
    }

    private fun verifiserAtMigreringIkkeErStartet(
        fagsakId: UUID,
        tasksFørKall: List<Long>,
    ) {
        assertThat(behandlingRepository.count()).isZero
        assertThat(fagsakRepository.count()).isEqualTo(1)
        assertThat(fagsakService.hentFagsak(fagsakId).migrert).isFalse
        assertThat(taskService.findAll().map { it.id }).containsExactlyInAnyOrderElementsOf(tasksFørKall)
        verify(exactly = 0) { infotrygdReplikaClient.hentSammenslåttePerioder(any()) }
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }
    }

    private fun migrer(
        fagsakPersonId: UUID,
        sti: String,
    ): ResponseEntity<Ressurs<UUID>> =
        testRestTemplate.exchange(
            localhost("/api/migrering/$fagsakPersonId$sti"),
            HttpMethod.POST,
            HttpEntity(MigrerRequestDto(), headers),
        )

    private fun mockInfotrygdPerioder(utgifterBarnetilsyn: Int = 0) {
        val periode =
            InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(
                vedtakId = 1,
                stønadFom = YearMonth.of(2022, 7).atDay(1),
                stønadTom = YearMonth.of(2023, 5).atEndOfMonth(),
                utgifterBarnetilsyn = utgifterBarnetilsyn,
                barnIdenter = listOf(PdlClientConfig.BARN_FNR),
            )
        val opphørtPeriode =
            periode.copy(
                vedtakId = 2,
                opphørsdato = YearMonth.of(2023, 5).atEndOfMonth(),
                kode = InfotrygdEndringKode.OVERTFØRT_NY_LØSNING,
            )
        every { infotrygdReplikaClient.hentSammenslåttePerioder(any()) } returns
            InfotrygdPeriodeResponse(listOf(opphørtPeriode), listOf(opphørtPeriode), emptyList())
    }

    companion object {
        private const val OVERGANGSSTØNAD_STI = ""
        private const val BARNETILSYN_STI = "/barnetilsyn"
    }
}
