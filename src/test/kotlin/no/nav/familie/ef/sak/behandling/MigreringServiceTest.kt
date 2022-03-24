package no.nav.familie.ef.sak.behandling

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.migrering.MigreringService
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
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
import no.nav.familie.ef.sak.infrastruktur.config.IverksettClientMock
import no.nav.familie.ef.sak.infrastruktur.config.IverksettClientMock.Companion.mockSimulering
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.GjeldendeBarnRepository
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.tilbakekreving.dto.TilbakekrevingDto
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdAktivitetstype
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import no.nav.familie.prosessering.internal.TaskWorker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
    @Autowired private lateinit var vedtakService: VedtakService
    @Autowired private lateinit var tilkjentYtelseService: TilkjentYtelseService
    @Autowired private lateinit var taskRepository: TaskRepository
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired private lateinit var taskWorker: TaskWorker
    @Autowired private lateinit var simuleringsresultatRepository: SimuleringsresultatRepository
    @Autowired private lateinit var vedtaksbrevService: VedtaksbrevService
    @Autowired private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository
    @Autowired private lateinit var stegService: StegService
    @Autowired private lateinit var tilbakekrevingService: TilbakekrevingService
    @Autowired private lateinit var rolleConfig: RolleConfig
    @Autowired private lateinit var iverksettClient: IverksettClient
    @Autowired private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient
    @Autowired private lateinit var gjeldendeBarnRepository: GjeldendeBarnRepository

    private val periodeFraMåned = YearMonth.now().minusMonths(10)
    private val opphørsmåned = YearMonth.now()
    private val migrerFraDato = YearMonth.now().plusMonths(1)
    private val til = YearMonth.now().plusMonths(1)
    private val forventetInntekt = BigDecimal.ZERO
    private val samordningsfradrag = BigDecimal.ZERO
    private lateinit var fagsak: Fagsak

    @BeforeEach
    internal fun setUp() {
        //Vid migrering forventer vi OK_MOT_OPPDRAG, vid revurdering forventer vi OK
        val responseFraInfotrygd: Queue<IverksettStatus> = LinkedList(listOf(IverksettStatus.OK_MOT_OPPDRAG, IverksettStatus.OK))
        every { iverksettClient.hentStatus(any()) } answers {
            responseFraInfotrygd.poll()
        }
        fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)
        mockSimulering(iverksettClient)
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
        with(vedtakService.hentVedtak(migrering.id)) {
            val perioder = this.perioder!!.perioder
            assertThat(perioder).hasSize(1)
            assertThat(perioder[0].aktivitet).isEqualTo(AktivitetType.MIGRERING)
            assertThat(perioder[0].periodeType).isEqualTo(VedtaksperiodeType.MIGRERING)
        }
        assertThat(simuleringsresultatRepository.findByIdOrNull(migrering.id)).isNotNull
        verifiserVurderinger(migrering)
    }

    @Test
    internal fun `skal sette aktivitet til reell arbeidssøker hvis aktivitet i infotrygd er reell arbeidssøker`() {
        val migrering = opprettOgIverksettMigrering(erReellArbeidssøker = true)
        with(vedtakService.hentVedtak(migrering.id)) {
            val perioder = this.perioder!!.perioder
            assertThat(perioder).hasSize(1)
            assertThat(perioder[0].aktivitet).isEqualTo(AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER)
            assertThat(perioder[0].periodeType).isEqualTo(VedtaksperiodeType.MIGRERING)
        }
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
    internal fun `migrering feiler når man har etterbetaling`() {
        mockSimulering(iverksettClient, etterbetaling = 1)
        assertThatThrownBy { opprettOgIverksettMigrering() }
                .hasMessageContaining("Etterbetaling er 1")
    }

    @Test
    internal fun `migrering feiler når man har feilutbetaling`() {
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        mockSimulering(iverksettClient, feilutbetaling = 2)
        assertThatThrownBy { opprettOgIverksettMigrering() }
                .hasMessageContaining("Feilutbetaling er 2")
    }

    @Test
    internal fun `hentMigreringInfo - historisk periode`() {
        val startdato = YearMonth.now().minusYears(1).atDay(1)
        val sluttMåned = opphørsmåned.minusMonths(2)
        val periode = InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(stønadFom = startdato,
                                                                   stønadTom = sluttMåned.atEndOfMonth(),
                                                                   beløp = 1)
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(periode), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isTrue
        assertThat(migreringInfo.stønadFom).isEqualTo(sluttMåned)
        assertThat(migreringInfo.stønadTom).isEqualTo(sluttMåned)
    }

    @Test
    internal fun `hentMigreringInfo - periode eldre enn 3 år - kan gå videre til journalføring`() {
        val stønadsmåned = YearMonth.now().minusYears(3).minusMonths(1)
        val periode = InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(stønadFom = stønadsmåned.atDay(1),
                                                                   stønadTom = stønadsmåned.atEndOfMonth(),
                                                                   beløp = 1)
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(periode), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.kanGåVidereTilJournalføring).isTrue
        assertThat(migreringInfo.årsak).contains("Kan ikke migrere når forrige utbetaling i infotrygd er mer enn 3 år tilbake")
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
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.årsak).isEqualTo("Har fler enn 1 (2) aktiv periode")
    }

    @Test
    internal fun `hentMigreringInfo - har kun perioder til og med denne måned i infotrygd`() {
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(InfotrygdPeriodeTestUtil.lagInfotrygdPeriode()), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isTrue
        assertThat(migreringInfo.stønadFom).isEqualTo(YearMonth.now())
        assertThat(migreringInfo.stønadTom).isEqualTo(YearMonth.now())
    }

    @Test
    internal fun `hentMigreringInfo - siste periode har 0 som beløp, migrerer fra måneden bak den`() {
        val periode = InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(beløp = 0, vedtakId = 1)
        val månedenFør = YearMonth.from(periode.stønadFom).minusMonths(1)
        val periode2 = InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(stønadFom = månedenFør.atDay(1),
                                                                    stønadTom = månedenFør.atEndOfMonth(),
                                                                    vedtakId = 2,
                                                                    beløp = 2)
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(periode, periode2), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isTrue
        assertThat(migreringInfo.stønadFom).isEqualTo(månedenFør)
        assertThat(migreringInfo.stønadTom).isEqualTo(månedenFør)
        assertThat(migreringInfo.beløpsperioder?.first()?.beløp?.toInt()).isEqualTo(19949)
    }

    @Test
    internal fun `hentMigreringInfo - sak inneholder annen ident`() {
        every { infotrygdReplikaClient.hentSaker(any()) } returns
                InfotrygdSakResponse(listOf(InfotrygdSak("2",
                                                         stønadType = OVERGANGSSTØNAD,
                                                         resultat = InfotrygdSakResultat.INNVILGET)))
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.årsak).contains("Finnes sak med annen personIdent for personen")
    }

    @Test
    internal fun `hentMigreringInfo - sak er åpen`() {
        every { infotrygdReplikaClient.hentSaker(any()) } returns
                InfotrygdSakResponse(listOf(InfotrygdSak("1",
                                                         stønadType = OVERGANGSSTØNAD,
                                                         resultat = InfotrygdSakResultat.ÅPEN_SAK)))
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.årsak).contains("Har åpen sak")
    }

    @Test
    internal fun `hentMigreringInfo - har perioder til og med neste måned i infotrygd`() {
        val nå = YearMonth.of(2021, 1)
        val stønadFom = nå.minusMonths(1).atDay(1)
        val stønadTomMåned = nå.plusMonths(3)
        val stønadTom = stønadTomMåned.atEndOfMonth()
        val infotrygdPeriode = InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(
                stønadFom = stønadFom,
                stønadTom = stønadTom,
                inntektsgrunnlag = 10,
                samordningsfradrag = 5)
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(listOf(infotrygdPeriode), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId, nå)

        assertThat(migreringInfo.kanMigreres).isTrue
        assertThat(migreringInfo.årsak).isNull()
        assertThat(migreringInfo.stønadFom).isEqualTo(nå)
        assertThat(migreringInfo.stønadTom).isEqualTo(stønadTomMåned)
        assertThat(migreringInfo.inntektsgrunnlag).isEqualTo(10)
        assertThat(migreringInfo.samordningsfradrag).isEqualTo(5)
        assertThat(migreringInfo.beløpsperioder).hasSize(1)

        val beløpsperiode = migreringInfo.beløpsperioder!![0]
        assertThat(beløpsperiode.beløp.toInt()).isEqualTo(18998)
        assertThat(beløpsperiode.periode.fradato).isEqualTo(nå.atDay(1))
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
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId, nå)

        assertThat(migreringInfo.kanMigreres).isTrue
        assertThat(migreringInfo.stønadFom).isEqualTo(stønadFom)
        assertThat(migreringInfo.stønadFom).isEqualTo(stønadTom)
    }

    @Test
    internal fun `hentMigreringInfo - har saker, men ingen perioder i Infotrygd, kan gå videre til journalføring`() {
        val nå = YearMonth.of(2021, 1)
        every { infotrygdReplikaClient.hentSaker(any()) } returns
                InfotrygdSakResponse(listOf(InfotrygdSak("1",
                                                         stønadType = OVERGANGSSTØNAD,
                                                         resultat = InfotrygdSakResultat.INNVILGET)))
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(emptyList(), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId, nå)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.kanGåVidereTilJournalføring).isTrue
        assertThat(migreringInfo.årsak).contains("Har ikke noen perioder å migrere")
    }

    @Test
    internal fun `hentMigreringInfo - har åpne saker, men ingen perioder i Infotrygd - kan ikke migreres`() {
        val nå = YearMonth.of(2021, 1)
        every { infotrygdReplikaClient.hentSaker(any()) } returns
                InfotrygdSakResponse(listOf(InfotrygdSak("1",
                                                         stønadType = OVERGANGSSTØNAD,
                                                         resultat = InfotrygdSakResultat.ÅPEN_SAK)))
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(emptyList(), emptyList(), emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId, nå)

        assertThat(migreringInfo.kanMigreres).isFalse
        assertThat(migreringInfo.kanGåVidereTilJournalføring).isFalse
        assertThat(migreringInfo.årsak).contains("Har åpen sak. ")
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

        val migreringInfo = migreringService.hentMigreringInfo(fagsak.fagsakPersonId)

        assertThat(migreringInfo.kanMigreres).isTrue
    }

    @Nested
    inner class BarnFinnesMedPåUttrekkTilOppretteOppgaver {

        @Test
        internal fun `skal finne barn på uttrekk til oppgaver etter migrering`() {
            opprettOgIverksettMigrering()
            assertThat(gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(OVERGANGSSTØNAD, LocalDate.now())).hasSize(2)
            assertThat(gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(OVERGANGSSTØNAD, LocalDate.now()))
                    .isEmpty()
        }

        @Test
        internal fun `skal finne barn på uttrekk til oppgaver når det har blitt laget en revurdering på barn uten å velge noen barn`() {
            val migrering = opprettOgIverksettMigrering()
            val revurdering = opprettRevurderingOgIverksett(migrering)
            assertThat(gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(OVERGANGSSTØNAD, LocalDate.now()))
                    .isEmpty()
            val migrerteBarn = gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(OVERGANGSSTØNAD, LocalDate.now())
            assertThat(migrerteBarn).hasSize(2)
            assertThat(migrerteBarn.map { it.behandlingId }.toSet()).containsExactly(revurdering.id)
        }

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
        val saksbehandling = saksbehandling(fagsak, revurdering)
        innvilgOgSendTilBeslutter(saksbehandling)
        godkjennTotrinnskontroll(behandlingService.hentSaksbehandling(revurdering.id))
        kjørTasks(erMigrering = false)
        return revurdering
    }

    private fun verifiserBehandlingErFerdigstilt(revurdering: Behandling) {
        val oppdatertRevurdering = behandlingService.hentBehandling(revurdering.id)
        assertThat(oppdatertRevurdering.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(oppdatertRevurdering.resultat).isEqualTo(BehandlingResultat.INNVILGET)
        assertThat(oppdatertRevurdering.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
    }

    private fun innvilgOgSendTilBeslutter(saksbehandling: Saksbehandling) {
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
            stegService.håndterBeregnYtelseForStønad(saksbehandling, innvilget)
            tilbakekrevingService.lagreTilbakekreving(TilbakekrevingDto(Tilbakekrevingsvalg.AVVENT, begrunnelse = ""),
                                                      saksbehandling.id)
            vedtaksbrevService.lagSaksbehandlerSanitybrev(saksbehandling, brevrequest, "brevMal")
            stegService.håndterSendTilBeslutter(behandlingService.hentSaksbehandling(saksbehandling.id))
        }
    }

    private fun godkjennTotrinnskontroll(saksbehandling: Saksbehandling) {
        testWithBrukerContext(preferredUsername = "Beslutter", groups = listOf(rolleConfig.beslutterRolle)) {
            vedtaksbrevService.lagBeslutterBrev(saksbehandling)
            stegService.håndterBeslutteVedtak(behandlingService.hentSaksbehandling(saksbehandling.id), BeslutteVedtakDto(true))
        }
    }

    private fun opprettOgIverksettMigrering(opphørsdato: YearMonth? = opphørsmåned,
                                            inntektsgrunnlag: BigDecimal = forventetInntekt,
                                            migrerFraDato: YearMonth = this.migrerFraDato,
                                            migrerTilDato: YearMonth = til,
                                            erReellArbeidssøker: Boolean = false,
                                            mockPerioder: () -> Unit = { mockPerioder(opphørsdato) }): Behandling {

        mockPerioder()

        val fagsak = fagsakService.hentEllerOpprettFagsak("1", OVERGANGSSTØNAD)
        val behandling = testWithBrukerContext(groups = listOf(rolleConfig.beslutterRolle)) {
            migreringService.opprettMigrering(fagsak,
                                              migrerFraDato,
                                              migrerTilDato,
                                              inntektsgrunnlag.toInt(),
                                              samordningsfradrag.toInt(),
                                              erReellArbeidssøker = erReellArbeidssøker)
        }

        kjørTasks()
        return behandling
    }

    /**
     * Mocker 2 vedtak, hvor vedtakId2 har høyest precedence, og setter opphørsdato på denne hvis det er type opphør
     */
    private fun mockPerioder(opphørsdato: YearMonth? = opphørsmåned,
                             stønadFom: YearMonth = periodeFraMåned,
                             stønadTom: YearMonth = til,
                             aktivitetstype: InfotrygdAktivitetstype = InfotrygdAktivitetstype.BRUKERKONTAKT) {
        val periode = InfotrygdPeriodeTestUtil.lagInfotrygdPeriode(vedtakId = 1,
                                                                   stønadFom = stønadFom.atDay(1),
                                                                   stønadTom = stønadTom.atEndOfMonth())
        val kodePeriode2 = opphørsdato?.let { InfotrygdEndringKode.OVERTFØRT_NY_LØSNING } ?: InfotrygdEndringKode.NY
        val periodeForKallNr2 = periode.copy(vedtakId = 2,
                                             opphørsdato = opphørsdato?.atEndOfMonth(),
                                             kode = kodePeriode2,
                                             aktivitetstype = aktivitetstype)
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