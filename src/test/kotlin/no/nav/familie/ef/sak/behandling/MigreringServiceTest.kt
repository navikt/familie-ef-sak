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
import no.nav.familie.ef.sak.behandlingsflyt.task.SjekkMigrertStatusIInfotrygdTask
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
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
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import no.nav.familie.prosessering.internal.TaskWorker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
    @Autowired private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

    private val periodeFraMåned = YearMonth.now().minusMonths(10)
    private val opphørsmåned = YearMonth.now()
    private val migrerFraDato = YearMonth.now().plusMonths(1)
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
        InfotrygdReplikaMock.resetMock(infotrygdReplikaClient)
    }

    @Test
    internal fun `skal opprette migrering og sende til iverksett`() {
        val migrering = opprettOgIverksettMigrering()

        with(tilkjentYtelseService.hentForBehandling(migrering.id).andelerTilkjentYtelse) {
            assertThat(this).hasSize(1)
            assertThat(this[0].stønadFom).isEqualTo(migrerFraDato.atDay(1))
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

    @Test
    internal fun `migrert behandling er ikke opphørt i Infotrygd`() {
        assertThatThrownBy { opprettOgIverksettMigrering(opphørsdato = null) }
                .hasMessageContaining(SjekkMigrertStatusIInfotrygdTask.TYPE)
                .hasCauseInstanceOf(TaskExceptionUtenStackTrace::class.java)
    }

    @Test
    internal fun `migrering med 0-beløp skal håndteres`() {
        every { iverksettClient.hentStatus(any()) } returns IverksettStatus.SENDT_TIL_OPPDRAG

        val migrering = opprettOgIverksettMigrering(inntektsgrunnlag = BigDecimal(1_000_000))

        verifiserBehandlingErFerdigstilt(migrering)
        with(tilkjentYtelseService.hentForBehandling(migrering.id).andelerTilkjentYtelse) {
            assertThat(this).hasSize(1)
            assertThat(this[0].stønadFom).isEqualTo(migrerFraDato.atDay(1))
            assertThat(this[0].stønadTom).isEqualTo(til.atEndOfMonth())
            assertThat(this[0].beløp).isEqualTo(0)
        }
    }

    @Test
    internal fun `hentMigreringInfo - har ingen perioder fra neste måned i infotrygd`() {
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(emptyList(), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", Stønadstype.OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.årsak).isEqualTo("Har 0 aktive perioder")
    }

    @Test
    internal fun `hentMigreringInfo - har fler enn 1 perioder fra neste måned i infotrygd`() {
        val stønadTom = YearMonth.now().plusMonths(4).atEndOfMonth()
        val infotrygdPeriode = InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(stønadTom = stønadTom, beløp = 1)
        val infotrygdPeriode2 =
                InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(
                        stønadFom = stønadTom.plusDays(1),
                        stønadTom = LocalDate.now().plusMonths(6),
                        beløp = 2
                )
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(infotrygdPeriode, infotrygdPeriode2), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", Stønadstype.OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.årsak).isEqualTo("Har fler enn 1 (2) aktiv periode")
    }

    @Test
    internal fun `hentMigreringInfo - har kun perioder til og med denne måned i infotrygd`() {
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(InfotrygdPeriodeTestUtil.lagInfotrygdPeriode()), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", Stønadstype.OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.årsak).isEqualTo("Har 0 aktive perioder")
    }

    @Test
    internal fun `hentMigreringInfo - sak inneholder annen ident`() {
        every { infotrygdReplikaClient.hentSaker(any()) } returns
                InfotrygdSakResponse(listOf(InfotrygdSak("2",
                                                         stønadType = StønadType.OVERGANGSSTØNAD,
                                                         resultat = InfotrygdSakResultat.INNVILGET)))
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", Stønadstype.OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.årsak).contains("Finnes sak med annen personIdent for personen")
    }

    @Test
    internal fun `hentMigreringInfo - sak er åpen`() {
        every { infotrygdReplikaClient.hentSaker(any()) } returns
                InfotrygdSakResponse(listOf(InfotrygdSak("1",
                                                         stønadType = StønadType.OVERGANGSSTØNAD,
                                                         resultat = InfotrygdSakResultat.ÅPEN_SAK)))
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", Stønadstype.OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.årsak).contains("Har åpen sak")
    }

    @Test
    internal fun `hentMigreringInfo - har perioder til og med neste måned i infotrygd`() {
        val nå = YearMonth.of(2021, 1)
        val nesteMåned = nå.plusMonths(1)
        val stønadFom = nå.minusMonths(1).atDay(1)
        val stønadTom = nesteMåned.atEndOfMonth()
        val infotrygdPeriode =
                InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(
                        stønadFom = stønadFom,
                        stønadTom = stønadTom,
                        inntektsgrunnlag = 10,
                        samordningsfradrag = 5
                )
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(infotrygdPeriode), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", Stønadstype.OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId, nå)

        val forventetStønadFom = nesteMåned.atDay(1)
        assertThat(migreringInfo.kanMigreres).isTrue
        assertThat(migreringInfo.årsak).isNull()
        assertThat(migreringInfo.stønadFom).isEqualTo(nesteMåned)
        assertThat(migreringInfo.stønadTom).isEqualTo(nesteMåned)
        assertThat(migreringInfo.inntektsgrunnlag).isEqualTo(10)
        assertThat(migreringInfo.samordningsfradrag).isEqualTo(5)
        assertThat(migreringInfo.beløpsperioder).hasSize(1)

        val beløpsperiode = migreringInfo.beløpsperioder!![0]
        assertThat(beløpsperiode.beløp.toInt()).isEqualTo(18998)
        assertThat(beløpsperiode.periode.fradato).isEqualTo(forventetStønadFom)
        assertThat(beløpsperiode.periode.tildato).isEqualTo(stønadTom)
    }

    @Test
    internal fun `hentMigreringInfo - har periode frem i tiden`() {
        val nå = YearMonth.of(2021, 1)
        val stønadFom = nå.plusMonths(10)
        val stønadTom = nå.plusMonths(10)
        val infotrygdPeriode =
                InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(
                        stønadFom = stønadFom.atDay(1),
                        stønadTom = stønadTom.atEndOfMonth(),
                        inntektsgrunnlag = 10,
                        samordningsfradrag = 5
                )
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(infotrygdPeriode), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", Stønadstype.OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId, nå)

        assertThat(migreringInfo.kanMigreres).isTrue
        assertThat(migreringInfo.stønadFom).isEqualTo(stønadFom)
        assertThat(migreringInfo.stønadFom).isEqualTo(stønadTom)
    }

    @Test
    internal fun `hentMigreringInfo - har ikke fagsak, men fagsakPerson`() {
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(
                        InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(
                                stønadFom = YearMonth.now().plusMonths(1).atDay(1),
                                stønadTom = YearMonth.now().plusMonths(1).atEndOfMonth(),
                                inntektsgrunnlag = 10,
                                samordningsfradrag = 5
                        )
                ), emptyList(), emptyList())
        val fagsakPerson = testoppsettService.opprettPerson("1")

        val migreringInfo = migreringService.hentMigreringInfo(fagsakPerson.id)

        assertThat(migreringInfo.kanMigreres).isTrue
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
                                            kravMottatt = LocalDate.now(),
                                            emptyList())
        val revurdering = testWithBrukerContext { revurderingService.opprettRevurderingManuelt(revurderingDto) }
        innvilgOgSendTilBeslutter(revurdering)
        godkjennTotrinnskontroll(revurdering)
        kjørTasks(erMigrering = false)
        return revurdering
    }

    private fun verifiserBehandlingErFerdigstilt(revurdering: Behandling) {
        val oppdatertRevurdering = behandlingService.hentBehandling(revurdering.id)
        assertThat(oppdatertRevurdering.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(oppdatertRevurdering.resultat).isEqualTo(BehandlingResultat.INNVILGET)
        assertThat(oppdatertRevurdering.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
    }

    private fun innvilgOgSendTilBeslutter(behandling: Behandling) {
        val vedtaksperiode = VedtaksperiodeDto(årMånedFra = migrerFraDato,
                                               årMånedTil = til,
                                               aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
                                               periodeType = VedtaksperiodeType.HOVEDPERIODE)

        val inntekt = Inntekt(migrerFraDato, forventetInntekt = forventetInntekt, samordningsfradrag = samordningsfradrag)
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
            vedtaksbrevService.lagSaksbehandlerSanitybrev(behandling.id, brevrequest, "brevMal")
            stegService.håndterSendTilBeslutter(behandlingService.hentBehandling(behandling.id))
        }
    }

    private fun godkjennTotrinnskontroll(behandling: Behandling) {
        testWithBrukerContext(preferredUsername = "Beslutter", groups = listOf(rolleConfig.beslutterRolle)) {
            vedtaksbrevService.lagBeslutterBrev(behandling.id)
            stegService.håndterBeslutteVedtak(behandlingService.hentBehandling(behandling.id), BeslutteVedtakDto(true))
        }
    }

    private fun opprettOgIverksettMigrering(opphørsdato: YearMonth? = opphørsmåned,
                                            inntektsgrunnlag: BigDecimal = forventetInntekt): Behandling {
        mockPerioder(opphørsdato)

        val fagsak = fagsakService.hentEllerOpprettFagsak("1", Stønadstype.OVERGANGSSTØNAD)
        val behandling = testWithBrukerContext(groups = listOf(rolleConfig.beslutterRolle)) {
            migreringService.opprettMigrering(fagsak, migrerFraDato, til, inntektsgrunnlag.toInt(), samordningsfradrag.toInt())
        }

        kjørTasks()
        return behandling
    }

    /**
     * Mocker 2 vedtak, hvor vedtakId2 har høyest precedence, og setter opphørsdato på denne hvis det er type opphør
     */
    private fun mockPerioder(opphørsdato: YearMonth?) {
        val periode = InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(vedtakId = 1,
                                                                   stønadFom = periodeFraMåned.atDay(1),
                                                                   stønadTom = til.atEndOfMonth())
        val kodePeriode2 = opphørsdato?.let { InfotrygdEndringKode.OVERTFØRT_NY_LØSNING } ?: InfotrygdEndringKode.NY
        val periodeForKallNr2 = periode.copy(vedtakId = 2,
                                             opphørsdato = opphørsdato?.atEndOfMonth(),
                                             kode = kodePeriode2)
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(periode), emptyList(), emptyList()) andThen
                InfotrygdPeriodeResponse(listOf(periodeForKallNr2), emptyList(), emptyList())
    }

    private fun kjørTasks(erMigrering: Boolean = true) {
        listOfNotNull(PollStatusFraIverksettTask.TYPE,
                      LagSaksbehandlingsblankettTask.TYPE,
                      FerdigstillBehandlingTask.TYPE,
                      PubliserVedtakshendelseTask.TYPE,
                      if (erMigrering) SjekkMigrertStatusIInfotrygdTask.TYPE else null).forEach { type ->
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