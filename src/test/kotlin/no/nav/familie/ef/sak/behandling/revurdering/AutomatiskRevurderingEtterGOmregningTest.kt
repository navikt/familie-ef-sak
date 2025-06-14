package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling.revurdering

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.revurdering.AutomatiskRevurderingService
import no.nav.familie.ef.sak.behandling.revurdering.BehandleAutomatiskInntektsendringTask
import no.nav.familie.ef.sak.behandling.revurdering.PayloadBehandleAutomatiskInntektsendringTask
import no.nav.familie.ef.sak.behandling.revurdering.RevurderingService
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.YEAR_MONTH_MAX
import no.nav.familie.ef.sak.infrastruktur.config.InntektClientMock
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.lagInntektResponseFraMånedsinntekter
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.testutil.VedtakHelperService
import no.nav.familie.ef.sak.testutil.VilkårHelperService
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

class AutomatiskRevurderingEtterGOmregningTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var oppgaveService: OppgaveService

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
    private lateinit var vilkårHelperService: VilkårHelperService

    @Autowired
    private lateinit var vedtakHelperService: VedtakHelperService

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
        gOmregningTestUtil.gOmregne(behandlingId, fagsakId)

        val gOmregningBehandling = behandlingRepository.findByFagsakId(fagsakId).last()
        behandlingRepository.update(gOmregningBehandling.copy(status = BehandlingStatus.FERDIGSTILT))

        val personIdent = "321"
        val behandling = behandling(fagsak, resultat = BehandlingResultat.IKKE_SATT, status = BehandlingStatus.UTREDES)

        behandlingRepository.insert(behandling)
        vilkårHelperService.opprettVilkår(behandling)

        val innmeldtMånedsinntekt = listOf(20_000, 24_000, 24_000, 28_000, 28_000, 30_000, 30_000)
        val vedtakTom = YearMonth.now().plusMonths(11)

        val forventetInntektIVedtak =
            mapOf(
                (YearMonth.now().minusMonths(innmeldtMånedsinntekt.size.toLong()) to 24_000),
            )
        val vedtak = vedtak(forventetInntektIVedtak, vedtakTom)
        vedtakHelperService.ferdigstillVedtak(vedtak, behandling, fagsak)

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
        ).isEqualTo(YearMonth.now().minusMonths(3))
        assertThat(
            oppdatertVedtak.perioder
                ?.perioder
                ?.first()
                ?.periode
                ?.tom,
        ).isEqualTo(vedtakTom)

        val oppdatertInntekt = oppdatertVedtak.inntekter?.inntekter ?: emptyList()
        assertThat(oppdatertInntekt.size).isEqualTo(4)
        assertThat(oppdatertVedtak.periodeBegrunnelse).isEqualTo("Overgangsstønaden endres fra måneden etter minst 10 prosent økning i inntekt.")
        assertThat(oppdatertVedtak.inntektBegrunnelse?.replace('\u00A0', ' ')).isEqualTo(forventetInntektsbegrunnelse) // Replace non-breaking space -> space
        val gjennomsnittSiste3Mnd = (28_000 + 30_000 + 30_000) / 3

        val forventedeInntektsperioderINyttVedtak =
            listOf(
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(3), YearMonth.now().minusMonths(3)), BigDecimal(28_000)),
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(2), YearMonth.now().minusMonths(2)), BigDecimal(30_000)),
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(1), YearMonth.now().minusMonths(1)), BigDecimal(30_000)),
                inntektsperiode(Månedsperiode(YearMonth.now(), YEAR_MONTH_MAX), BigDecimal(gjennomsnittSiste3Mnd)),
            )

        assertThat(oppdatertInntekt).isEqualTo(forventedeInntektsperioderINyttVedtak)
    }

    val forventetInntektsbegrunnelse =
        """
        Forventet årsinntekt fra februar 2025: 288 000 kroner.
        - 10 % opp: 26 400 kroner per måned.
        - 10 % ned: 21 600 kroner per måned.
        
        Inntekten i februar 2025 er 28 000 kroner. Inntekten har økt minst 10 prosent denne måneden. Stønaden beregnes på nytt fra måneden etter.
           
        Fra og med juni 2025 er stønaden beregnet ut ifra gjennomsnittlig inntekt i mars, april og mai.
        """.trimIndent()
}
