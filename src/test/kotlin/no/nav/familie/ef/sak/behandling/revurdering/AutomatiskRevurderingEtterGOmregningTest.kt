package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.revurdering.AutomatiskRevurderingService
import no.nav.familie.ef.sak.behandling.revurdering.BehandleAutomatiskInntektsendringTask
import no.nav.familie.ef.sak.behandling.revurdering.PayloadBehandleAutomatiskInntektsendringTask
import no.nav.familie.ef.sak.behandling.revurdering.RevurderingService
import no.nav.familie.ef.sak.behandling.revurdering.tilNorskFormat
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.YEAR_MONTH_MAX
import no.nav.familie.ef.sak.felles.util.månedTilNorskFormat
import no.nav.familie.ef.sak.infrastruktur.config.InntektClientMock
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.lagInntektResponseFraMånedsinntekter
import no.nav.familie.ef.sak.testutil.JsonFilUtil.Companion.lesFil
import no.nav.familie.ef.sak.testutil.JsonFilUtil.Companion.oppdaterMåneder
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID
import kotlin.text.replace

class AutomatiskRevurderingEtterGOmregningTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var revurderingService: RevurderingService

    @Autowired
    private lateinit var featureToggleService: FeatureToggleService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Autowired
    private lateinit var årsakRevurderingsRepository: ÅrsakRevurderingsRepository

    @Autowired
    private lateinit var automatiskRevurderingService: AutomatiskRevurderingService

    @Autowired
    lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    private lateinit var inntektClientMock: InntektClientMock

    @Autowired
    private lateinit var gOmregningTestUtil: GOmregningTestUtil

    val fagsakId = UUID.fromString("3549f9e2-ddd1-467d-82be-bfdb6c7f07e1")
    val behandlingId = UUID.fromString("39c7dc82-adc1-43db-a6f9-64b8e4352ff6")

    private val personIdent = "3"
    private val fagsak = fagsak(id = fagsakId, identer = fagsakpersoner(setOf(personIdent)))

    private lateinit var behandleAutomatiskInntektsendringTask: BehandleAutomatiskInntektsendringTask

    @BeforeEach
    fun setup() {
        // testoppsettService.lagreFagsak(fagsak)
        behandleAutomatiskInntektsendringTask =
            BehandleAutomatiskInntektsendringTask(
                revurderingService = revurderingService,
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                årsakRevurderingsRepository = årsakRevurderingsRepository,
                automatiskRevurderingService = automatiskRevurderingService,
                featureToggleService = featureToggleService,
            )
    }

    @Test
    fun `Full kjøring - siste behandling er g-omregning`() {
        gOmregningTestUtil.gOmregne(behandlingId, fagsakId) // 23023 * 12 = 276 000

        val gOmregningBehandling = behandlingRepository.findByFagsakId(fagsakId).first { it.erGOmregning() }
        behandlingRepository.update(gOmregningBehandling.copy(status = BehandlingStatus.FERDIGSTILT))

        val personIdent = "321"

        val innmeldtMånedsinntekt = listOf(20_000, 24_000, 24_000, 28_000, 28_000, 28_000, 30_000, 30_000)

        val payload = PayloadBehandleAutomatiskInntektsendringTask(personIdent, "2025-20")
        val opprettetTask = BehandleAutomatiskInntektsendringTask.opprettTask(objectMapper.writeValueAsString(payload))
        val inntektResponse = lagInntektResponseFraMånedsinntekter(innmeldtMånedsinntekt)

        every { inntektClientMock.inntektClient().hentInntekt("321", any(), any()) } returns inntektResponse

        behandleAutomatiskInntektsendringTask.doTask(opprettetTask)

        val revurdering = behandlingService.hentBehandlinger(fagsak.id).last()
        val oppdatertVedtak = vedtakService.hentVedtak(revurdering.id)

        assertThat(
            oppdatertVedtak.perioder
                ?.perioder
                ?.first()
                ?.periode
                ?.fom,
        ).isEqualTo(YearMonth.now().minusMonths(4))
        assertThat(
            oppdatertVedtak.perioder
                ?.perioder
                ?.first()
                ?.periode
                ?.tom,
        ).isEqualTo(YearMonth.of(2025, 12))

        val oppdatertInntekt = oppdatertVedtak.inntekter?.inntekter ?: emptyList()
        assertThat(oppdatertInntekt.size).isEqualTo(3)
        assertThat(oppdatertVedtak.periodeBegrunnelse).isEqualTo("Behandlingen er opprettet automatisk fordi inntekten har økt. Overgangsstønaden endres fra måneden etter at inntekten har økt minst 10 prosent.")
        assertThat(oppdatertVedtak.inntektBegrunnelse?.replace('\u00A0', ' ')).isEqualTo(forventetInntektsbegrunnelseMedGOmregning) // Replace non-breaking space -> space
        val gjennomsnittSiste3Mnd = (28_000 + 30_000 + 30_000) / 3

        val forventedeInntektsperioderINyttVedtak =
            listOf(
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(4), YearMonth.now().minusMonths(3)), BigDecimal(28_000)),
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(2), YearMonth.now().minusMonths(1)), BigDecimal(30_000)),
                inntektsperiode(Månedsperiode(YearMonth.now(), YEAR_MONTH_MAX), BigDecimal(gjennomsnittSiste3Mnd)),
            )

        assertThat(oppdatertInntekt).isEqualTo(forventedeInntektsperioderINyttVedtak)
    }

    @Test
    fun `Siste behandling er g-omregning og vedtaksperiode før g-omregning har samme fom-dato som g-omregning`() {
        gOmregningTestUtil.gOmregne(behandlingId, fagsakId, YearMonth.of(YearMonth.now().year, 5))

        val gOmregningBehandling = behandlingRepository.findByFagsakId(fagsakId).first { it.erGOmregning() }
        behandlingRepository.update(gOmregningBehandling.copy(status = BehandlingStatus.FERDIGSTILT))

        val personIdent = "321"

        val innmeldtMånedsinntekt = listOf(20_000, 24_000, 24_000, 28_000, 28_000, 30_000, 30_000)

        val payload = PayloadBehandleAutomatiskInntektsendringTask(personIdent, "2025-20")
        val opprettetTask = BehandleAutomatiskInntektsendringTask.opprettTask(objectMapper.writeValueAsString(payload))
        val inntektResponse = lagInntektResponseFraMånedsinntekter(innmeldtMånedsinntekt)

        every { inntektClientMock.inntektClient().hentInntekt("321", any(), any()) } returns inntektResponse

        behandleAutomatiskInntektsendringTask.doTask(opprettetTask)

        val revurdering = behandlingService.hentBehandlinger(fagsak.id).last()
        val oppdatertVedtak = vedtakService.hentVedtak(revurdering.id)

        assertThat(
            oppdatertVedtak.perioder
                ?.perioder
                ?.first()
                ?.periode
                ?.fom,
        ).isEqualTo(YearMonth.of(YearMonth.now().year, 6))
        assertThat(
            oppdatertVedtak.perioder
                ?.perioder
                ?.first()
                ?.periode
                ?.tom,
        ).isEqualTo(YearMonth.of(YearMonth.now().year + 1, 12))
    }

    val førstegangsbehandlingFom =
        Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fom
            .minusMonths(2)

    @Test
    fun `Inntektsendring samme måned som g-omregning`() {
        gOmregningTestUtil.gOmregne(behandlingId, fagsakId, førstegangsbehandlingFom, 5168)

        val gOmregningBehandling = behandlingRepository.findByFagsakId(fagsakId).first { it.erGOmregning() }
        behandlingRepository.update(gOmregningBehandling.copy(status = BehandlingStatus.FERDIGSTILT))

        val personIdent = "321"

        val mockResponseJson =
            oppdaterMåneder(
                lesFil("json/inntekt/InntektResponseØktInntektSamtidigMedGOmregning.json"),
            )

        val mockResponse = objectMapper.readValue<InntektResponse>(mockResponseJson)
        every { inntektClientMock.inntektClient().hentInntekt(any(), any(), any()) } returns mockResponse

        val payload = PayloadBehandleAutomatiskInntektsendringTask(personIdent, "2025-20")
        val opprettetTask = BehandleAutomatiskInntektsendringTask.opprettTask(objectMapper.writeValueAsString(payload))

        behandleAutomatiskInntektsendringTask.doTask(opprettetTask)

        val revurdering = behandlingService.hentBehandlinger(fagsak.id).last()
        val oppdatertVedtak = vedtakService.hentVedtak(revurdering.id)

        assertThat(
            oppdatertVedtak.perioder
                ?.perioder
                ?.first()
                ?.periode
                ?.fom,
        ).isEqualTo(YearMonth.of(YearMonth.now().year, 6))
        assertThat(
            oppdatertVedtak.perioder
                ?.perioder
                ?.first()
                ?.periode
                ?.tom,
        ).isEqualTo(YearMonth.of(YearMonth.now().year + 1, 12))

        assertThat(oppdatertVedtak.inntektBegrunnelse?.replace('\u00A0', ' ')).isEqualTo(forventetInntektsbegrunnelseMedGOmregningSamtidigMedInntektsøkning) // Replace non-breaking space -> space
    }

    val forventetInntektsbegrunnelseMedGOmregningSamtidigMedInntektsøkning =
        """
        Periode som er kontrollert: ${førstegangsbehandlingFom.tilNorskFormat()} til ${
            YearMonth.now().minusMonths(1).tilNorskFormat()}.
        
        Forventet årsinntekt i ${YearMonth.of(YearMonth.now().year, 5).tilNorskFormat()}: 65 000 kroner.
        - 10 % opp: 5 958 kroner per måned.
        - 10 % ned: 4 875 kroner per måned.
        
        Inntekten i ${YearMonth.of(YearMonth.now().year, 5).tilNorskFormat()} er 11 682 kroner. Inntekten har økt minst 10 prosent denne måneden og alle månedene etter dette. Stønaden beregnes på nytt fra måneden etter 10 prosent økning.
        
        Har lagt til grunn faktisk inntekt bakover i tid. Fra og med ${YearMonth.now().tilNorskFormat()} er stønaden beregnet ut ifra gjennomsnittlig inntekt for ${YearMonth.now().minusMonths(3).månedTilNorskFormat()}, ${YearMonth.now().minusMonths(2).månedTilNorskFormat()} og ${YearMonth.now().minusMonths(1).månedTilNorskFormat()}. Bruker har fått utbetalt feriepenger i løpet av siste tre måneder. Disse er ikke tatt med i beregningen av forventet inntekt.
        
        A-inntekt er lagret.
        """.trimIndent()

    val forventetInntektsbegrunnelseMedGOmregning =
        """
        Periode som er kontrollert: ${YearMonth.now().minusMonths(12).tilNorskFormat()} til ${
            YearMonth.now().minusMonths(1).tilNorskFormat()}.
        
        Forventet årsinntekt i ${YearMonth.of(YearMonth.now().year, 4).tilNorskFormat()}: 276 000 kroner.
        - 10 % opp: 25 300 kroner per måned.
        - 10 % ned: 20 700 kroner per måned.
        
        Forventet årsinntekt fra ${YearMonth.of(YearMonth.now().year, 5).tilNorskFormat()}: 289 600 kroner (G-omregning).
        - 10 % opp: 26 546 kroner per måned.
        - 10 % ned: 21 720 kroner per måned.
        
        Inntekten i ${YearMonth.of(YearMonth.now().year, 4).tilNorskFormat()} er 28 000 kroner. Inntekten har økt minst 10 prosent denne måneden og alle månedene etter dette. Stønaden beregnes på nytt fra måneden etter 10 prosent økning.
        
        Har lagt til grunn faktisk inntekt bakover i tid. Fra og med ${YearMonth.now().tilNorskFormat()} er stønaden beregnet ut ifra gjennomsnittlig inntekt for ${YearMonth.now().minusMonths(3).månedTilNorskFormat()}, ${YearMonth.now().minusMonths(2).månedTilNorskFormat()} og ${YearMonth.now().minusMonths(1).månedTilNorskFormat()}.
        
        A-inntekt er lagret.
        """.trimIndent()
}
