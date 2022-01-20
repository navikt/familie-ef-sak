package no.nav.familie.ef.sak.behandling

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillBehandlingTask
import no.nav.familie.ef.sak.behandlingsflyt.task.LagSaksbehandlingsblankettTask
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.behandlingsflyt.task.PubliserVedtakshendelseTask
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infrastruktur.config.IverksettClientMock
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.tilbakekreving.dto.TilbakekrevingDto
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.internal.TaskWorker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.LinkedList
import java.util.Queue

internal class MigreringServiceTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakService: FagsakService
    @Autowired private lateinit var behandlingService: BehandlingService
    @Autowired private lateinit var revurderingService: RevurderingService
    @Autowired private lateinit var migreringService: MigreringService
    @Autowired private lateinit var tilkjentYtelseService: TilkjentYtelseService
    @Autowired private lateinit var taskRepository: TaskRepository
    @Autowired private lateinit var taskWorker: TaskWorker
    @Autowired private lateinit var simuleringsresultatRepository: SimuleringsresultatRepository
    @Autowired private lateinit var vedtaksbrevService: VedtaksbrevService
    @Autowired private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository
    @Autowired private lateinit var stegService: StegService
    @Autowired private lateinit var tilbakekrevingService: TilbakekrevingService
    @Autowired private lateinit var rolleConfig: RolleConfig
    @Autowired private lateinit var iverksettClient: IverksettClient

    private val fra = YearMonth.now()
    private val til = YearMonth.now().plusMonths(1)
    private val forventetInntekt = BigDecimal.ZERO
    private val samordningsfradrag = BigDecimal.ZERO

    @BeforeEach
    internal fun setUp() {
        //Vid migrering forventer vi OK_MOT_OPPDRAG, vid revurdering forventer vi OK
        val responseFraInfotrygd: Queue<IverksettStatus> = LinkedList(listOf(IverksettStatus.OK_MOT_OPPDRAG, IverksettStatus.OK))
        every { iverksettClient.hentStatus(any()) } answers {
            responseFraInfotrygd.poll()
        }
    }

    @AfterEach
    internal fun tearDown() {
        IverksettClientMock.clearMock(iverksettClient)
    }

    @Test
    internal fun `skal opprette migrering og sende til iverksett`() {
        val migrering = opprettOgIverksettMigrering()

        with(tilkjentYtelseService.hentForBehandling(migrering.id).andelerTilkjentYtelse) {
            assertThat(this).hasSize(1)
            assertThat(this[0].stønadFom).isEqualTo(fra.atDay(1))
            assertThat(this[0].stønadTom).isEqualTo(til.atEndOfMonth())
        }
        with(behandlingService.hentBehandling(migrering.id)) {
            assertThat(this.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
            assertThat(this.resultat).isEqualTo(BehandlingResultat.INNVILGET)
            assertThat(this.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        }
        assertThat(simuleringsresultatRepository.findByIdOrNull(migrering.id)).isNotNull
        verifiserVurderinger(migrering)
    }

    @Test
    internal fun `skal opprette revurering på migrering`() {
        val migrering = opprettOgIverksettMigrering()
        val revurdering = opprettRevurderingOgIverksett(migrering)

        verifiserBehandlingErFerdigstilt(revurdering)
    }

    private fun verifiserVurderinger(migrering: Behandling) {
        val vilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(migrering.id)
        val alleVurderingerManglerSvar = vilkårsvurderinger.flatMap { it.delvilkårsvurdering.delvilkårsvurderinger }
                .flatMap { it.vurderinger }
                .all { it.svar == null }
        val erOpprettetAvSystem = vilkårsvurderinger.flatMap { listOf(it.sporbar.opprettetAv, it.sporbar.endret.endretAv) }
                .all { it == SikkerhetContext.SYSTEM_FORKORTELSE }
        assertThat(alleVurderingerManglerSvar).isTrue
        assertThat(erOpprettetAvSystem).isTrue
    }

    private fun opprettRevurderingOgIverksett(migrering: Behandling): Behandling {
        val revurderingDto = RevurderingDto(fagsakId = migrering.fagsakId,
                                            behandlingsårsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                                            kravMottatt = LocalDate.now())
        val revurdering = testWithBrukerContext { revurderingService.opprettRevurderingManuelt(revurderingDto) }
        innvilgOgSendTilBeslutter(revurdering)
        godkjennTotrinnskontroll(revurdering)
        kjørTasks()
        return revurdering
    }

    private fun verifiserBehandlingErFerdigstilt(revurdering: Behandling) {
        val oppdatertRevurdering = behandlingService.hentBehandling(revurdering.id)
        assertThat(oppdatertRevurdering.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(oppdatertRevurdering.resultat).isEqualTo(BehandlingResultat.INNVILGET)
        assertThat(oppdatertRevurdering.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
    }

    private fun innvilgOgSendTilBeslutter(behandling: Behandling) {
        val vedtaksperiode = VedtaksperiodeDto(årMånedFra = fra,
                                               årMånedTil = til,
                                               aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
                                               periodeType = VedtaksperiodeType.HOVEDPERIODE)

        val inntekt = Inntekt(fra, forventetInntekt = forventetInntekt, samordningsfradrag = samordningsfradrag)
        val innvilget = Innvilget(resultatType = ResultatType.INNVILGE,
                                  periodeBegrunnelse = null,
                                  inntektBegrunnelse = null,
                                  perioder = listOf(vedtaksperiode),
                                  inntekter = listOf(inntekt))
        val brevrequest = objectMapper.readTree("123")
        testWithBrukerContext(groups = listOf(rolleConfig.saksbehandlerRolle)) {
            stegService.håndterBeregnYtelseForStønad(behandling, innvilget)
            tilbakekrevingService.lagreTilbakekreving(TilbakekrevingDto(Tilbakekrevingsvalg.AVVENT, begrunnelse = ""),
                                                      behandling.id)
            vedtaksbrevService.lagSaksbehandlerBrev(behandling.id, brevrequest, "brevMal")
            stegService.håndterSendTilBeslutter(behandlingService.hentBehandling(behandling.id))
        }
    }

    private fun godkjennTotrinnskontroll(behandling: Behandling) {
        testWithBrukerContext(preferredUsername = "Beslutter", groups = listOf(rolleConfig.beslutterRolle)) {
            vedtaksbrevService.lagBeslutterBrev(behandling.id)
            stegService.håndterBeslutteVedtak(behandlingService.hentBehandling(behandling.id), BeslutteVedtakDto(true))
        }
    }

    private fun opprettOgIverksettMigrering(): Behandling {
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", Stønadstype.OVERGANGSSTØNAD)
        val behandling = testWithBrukerContext(groups = listOf(rolleConfig.beslutterRolle)) {
            migreringService.opprettMigrering(fagsak, fra, til, forventetInntekt, samordningsfradrag)
        }

        kjørTasks()
        return behandling
    }

    private fun kjørTasks() {
        listOf(PollStatusFraIverksettTask.TYPE,
               LagSaksbehandlingsblankettTask.TYPE,
               FerdigstillBehandlingTask.TYPE,
               PubliserVedtakshendelseTask.TYPE).forEach { type ->
            try {
                val task = taskRepository.findAll()
                        .filter { it.status == Status.KLAR_TIL_PLUKK || it.status == Status.UBEHANDLET }
                        .single { it.type == type }
                taskWorker.markerPlukket(task.id)
                taskWorker.doActualWork(task.id)
            } catch (e: Exception) {
                throw RuntimeException("Feilet håntering av $type", e)
            }
        }
    }
}