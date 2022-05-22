package no.nav.familie.ef.sak.behandlingsflyt.steg

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTaskPayload
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

internal class BeslutteVedtakStegTest {

    private val taskRepository = mockk<TaskRepository>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val fagsakService = mockk<FagsakService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>()
    private val iverksettingDtoMapper = mockk<IverksettingDtoMapper>()
    private val iverksett = mockk<IverksettClient>()
    private val vedtakService = mockk<VedtakService>()
    private val vedtaksbrevService = mockk<VedtaksbrevService>()
    private val behandlingService = mockk<BehandlingService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val beslutteVedtakSteg = BeslutteVedtakSteg(taskRepository = taskRepository,
                                                        fagsakService = fagsakService,
                                                        oppgaveService = oppgaveService,
                                                        iverksettClient = iverksett,
                                                        iverksettingDtoMapper = iverksettingDtoMapper,
                                                        totrinnskontrollService = totrinnskontrollService,
                                                        behandlingService = behandlingService,
                                                        vedtakService = vedtakService,
                                                        vedtaksbrevService = vedtaksbrevService,
                                                        featureToggleService = featureToggleService)

    private val innloggetBeslutter = "sign2"

    private val fagsak = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD,
                                identer = setOf(PersonIdent(ident = "12345678901")))
    private val behandlingId = UUID.randomUUID()

    private val oppgave = Oppgave(id = UUID.randomUUID(),
                                  behandlingId = behandlingId,
                                  gsakOppgaveId = 123L,
                                  type = Oppgavetype.BehandleSak,
                                  erFerdigstilt = false)
    private lateinit var taskSlot: MutableList<Task>


    @BeforeEach
    internal fun setUp() {
        mockBrukerContext(innloggetBeslutter)
        taskSlot = mutableListOf()
        every {
            fagsakService.hentAktivIdent(any())
        } returns fagsak.hentAktivIdent()
        every {
            fagsakService.fagsakMedOppdatertPersonIdent(any())
        } returns fagsak
        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(any(), any()) } returns oppgave
        every { iverksettingDtoMapper.tilDto(any(), any()) } returns mockk()
        every { iverksett.iverksett(any(), any()) } just Runs
        every { iverksett.iverksettUtenBrev(any()) } just Runs
        every { vedtakService.hentVedtaksresultat(any()) } returns ResultatType.INNVILGE
        every { featureToggleService.isEnabled("familie.ef.sak.skal-validere-beslutterpdf-er-null") } returns false
        every { vedtakService.oppdaterBeslutter(any(), any()) } just Runs
        every { behandlingService.oppdaterResultatPåBehandling(any(), any()) } answers {
            behandling(fagsak, id = behandlingId, resultat = secondArg())
        }
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal opprette iverksettMotOppdragTask etter beslutte vedtak hvis godkjent`() {
        every { vedtakService.hentVedtaksresultat(behandlingId) } returns ResultatType.INNVILGE
        every { vedtaksbrevService.lagEndeligBeslutterbrev(any()) } returns Fil("123".toByteArray())

        val nesteSteg = utførTotrinnskontroll(godkjent = true)

        assertThat(nesteSteg).isEqualTo(StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        assertThat(taskSlot[0].type).isEqualTo(FerdigstillOppgaveTask.TYPE)
        assertThat(taskSlot[1].type).isEqualTo(PollStatusFraIverksettTask.TYPE)
        assertThat(taskSlot[2].type).isEqualTo(BehandlingsstatistikkTask.TYPE)
        assertThat(objectMapper.readValue<BehandlingsstatistikkTaskPayload>(taskSlot[2].payload).hendelse)
                .isEqualTo(Hendelse.BESLUTTET)
        verify(exactly = 1) { behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.INNVILGET) }
        verify(exactly = 1) { iverksett.iverksett(any(), any()) }
        verify(exactly = 0) { iverksett.iverksettUtenBrev(any()) }
    }

    @Test
    internal fun `skal ikke opprette iverksettMotOppdragTask dersom det revurderes med gammel g og ikke fra og med nyeste g`() {
        every { vedtakService.hentVedtaksresultat(behandlingId) } returns ResultatType.INNVILGE
        every { vedtaksbrevService.lagEndeligBeslutterbrev(any()) } returns Fil("123".toByteArray())
        every { tilkjentYtelseRepository.findByBehandlingId(behandlingId) } returns tilkjentYtelseEtterNyG()
        val nesteSteg = utførTotrinnskontroll(godkjent = true)

        assertThat(nesteSteg).isEqualTo(StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        assertThat(taskSlot[0].type).isEqualTo(FerdigstillOppgaveTask.TYPE)
        assertThat(taskSlot[1].type).isEqualTo(PollStatusFraIverksettTask.TYPE)
        assertThat(taskSlot[2].type).isEqualTo(BehandlingsstatistikkTask.TYPE)
        assertThat(objectMapper.readValue<BehandlingsstatistikkTaskPayload>(taskSlot[2].payload).hendelse)
                .isEqualTo(Hendelse.BESLUTTET)
        verify(exactly = 1) { behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.INNVILGET) }
        verify(exactly = 1) { iverksett.iverksett(any(), any()) }
        verify(exactly = 0) { iverksett.iverksettUtenBrev(any()) }
    }

    fun tilkjentYtelseEtterNyG() : TilkjentYtelse {
        val andeler = listOf(AndelTilkjentYtelse(beløp = 9500,
                                                 stønadFom = LocalDate.of(2022, 6, 1),
                                                 stønadTom = LocalDate.of(2022, 12, 31),
                                                 personIdent = "12345678901",
                                                 inntektsreduksjon = 0,
                                                 inntekt = 0,
                                                 samordningsfradrag = 0,
                                                 kildeBehandlingId = behandlingId))
        return TilkjentYtelse(behandlingId = behandlingId,
                              personident = "12345678901",
                              vedtakstidspunkt = LocalDateTime.now(),
                              startdato = LocalDate.of(2022, 6 ,1) ?: error("Må sette startdato"),
                              andelerTilkjentYtelse = andeler,
                              grunnbeløpsdato = LocalDate.of(2020, 5, 1))
    }


    @Test
    internal fun `skal opprette opprettBehandleUnderkjentVedtakOppgave etter beslutte vedtak hvis underkjent`() {
        every { vedtaksbrevService.slettVedtaksbrev(any()) } just Runs

        val nesteSteg = utførTotrinnskontroll(godkjent = false)

        val deserializedPayload = objectMapper.readValue<OpprettOppgaveTask.OpprettOppgaveTaskData>(taskSlot[1].payload)

        assertThat(nesteSteg).isEqualTo(StegType.SEND_TIL_BESLUTTER)
        assertThat(taskSlot[0].type).isEqualTo(FerdigstillOppgaveTask.TYPE)
        assertThat(taskSlot[1].type).isEqualTo(OpprettOppgaveTask.TYPE)
        assertThat(deserializedPayload.oppgavetype).isEqualTo(Oppgavetype.BehandleUnderkjentVedtak)
    }

    @Test
    internal fun `skal ikke sende brev hvis årsaken er korrigering uten brev`() {
        utførTotrinnskontroll(true, opprettSaksbehandling(BehandlingÅrsak.KORRIGERING_UTEN_BREV))

        verify(exactly = 0) { iverksett.iverksett(any(), any()) }
        verify(exactly = 1) { iverksett.iverksettUtenBrev(any()) }
    }

    private fun utførTotrinnskontroll(godkjent: Boolean, saksbehandling: Saksbehandling = opprettSaksbehandling()): StegType {
        return beslutteVedtakSteg.utførOgReturnerNesteSteg(saksbehandling,
                                                           BeslutteVedtakDto(godkjent = godkjent))
    }

    private fun opprettSaksbehandling(årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD) =
            saksbehandling(fagsak,
                           Behandling(id = behandlingId,
                                      fagsakId = fagsak.id,
                                      type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                      status = BehandlingStatus.FATTER_VEDTAK,
                                      steg = beslutteVedtakSteg.stegType(),
                                      resultat = BehandlingResultat.IKKE_SATT,
                                      årsak = årsak))
}