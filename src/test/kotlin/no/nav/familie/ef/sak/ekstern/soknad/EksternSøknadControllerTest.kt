package no.nav.familie.ef.sak.ekstern.soknad

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisteridentifikator
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.FolkeregisteridentifikatorStatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonStatus
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.innvilgetOgFerdigstilt
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.økonomi.DataGenerator
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.resttestclient.exchange
import org.springframework.cache.CacheManager
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate

class EksternSøknadControllerTest : OppslagSpringRunnerTest() {
    private val personident = PdlClientConfig.SØKER_FNR
    private val fagsakOvergangsstønad =
        fagsak(stønadstype = StønadType.OVERGANGSSTØNAD, identer = fagsakpersoner(setOf(personident)))
    private val fagsakBarnetilsyn =
        fagsak(stønadstype = StønadType.BARNETILSYN, identer = fagsakpersoner(setOf(personident)))
    private val fagsakSkolepenger =
        fagsak(stønadstype = StønadType.SKOLEPENGER, identer = fagsakpersoner(setOf(personident)))

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

    @Autowired
    private lateinit var historiskPensjonClient: HistoriskPensjonClient

    @Autowired
    private lateinit var pdlClient: PdlClient

    @Autowired
    @Qualifier("longCache")
    private lateinit var longCache: CacheManager

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(søkerToken(personident))
        InfotrygdReplikaMock.resetMock(infotrygdReplikaClient)
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
            InfotrygdPeriodeResponse(overgangsstønad = emptyList(), barnetilsyn = emptyList(), skolepenger = emptyList())
        every { historiskPensjonClient.hentHistoriskPensjonStatusForIdent(any(), any()) } returns
            HistoriskPensjonDto(HistoriskPensjonStatus.HAR_IKKE_HISTORIKK, "")
        every { pdlClient.hentSøker(any()) } returns PdlClientConfig.opprettPdlSøker()
        longCache.cacheNames.mapNotNull { longCache.getCache(it) }.forEach { it.clear() }
    }

    @Test
    fun `skal returnere JA for gjeldende innvilget vedtak på gammelt regelverk`() {
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        val behandling = behandlingRepository.insert(behandling(fagsakOvergangsstønad).innvilgetOgFerdigstilt())
        lagreAndel(behandling, beløp = 5000, tilOgMed = iDagPlusEttÅr())

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.JA)
    }

    @Test
    fun `skal returnere NEI for gjeldende innvilget vedtak på nytt regelverk`() {
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        val behandling =
            behandlingRepository.insert(
                behandling(fagsakOvergangsstønad).innvilgetOgFerdigstilt().copy(erRegelendring2026 = true),
            )
        lagreAndel(behandling, beløp = 5000, tilOgMed = iDagPlusEttÅr())

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.NEI)
    }

    @Test
    fun `skal returnere NEI når siste ferdigstilte behandling er på nytt regelverk selv om bruker finnes i Infotrygd`() {
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
            InfotrygdPeriodeResponse(
                overgangsstønad = listOf(lagInfotrygdPeriode(personident)),
                barnetilsyn = emptyList(),
                skolepenger = emptyList(),
            )
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        behandlingRepository.insert(
            behandling(fagsakOvergangsstønad).innvilgetOgFerdigstilt().copy(erRegelendring2026 = true),
        )

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.NEI)
    }

    @Test
    fun `skal returnere NEI når siste ferdigstilte behandling er på nytt regelverk selv om historisk pensjon har historikk`() {
        every { historiskPensjonClient.hentHistoriskPensjonStatusForIdent(any(), any()) } returns
            HistoriskPensjonDto(HistoriskPensjonStatus.HAR_HISTORIKK, "")
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        behandlingRepository.insert(
            behandling(fagsakOvergangsstønad).innvilgetOgFerdigstilt().copy(erRegelendring2026 = true),
        )

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.NEI)
    }

    @Test
    fun `skal bruke siste behandling slik at nyere gammelt vedtak gir JA selv om eldre behandling var på nytt regelverk`() {
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        behandlingRepository.insert(
            behandling(fagsakOvergangsstønad)
                .innvilgetOgFerdigstilt()
                .copy(erRegelendring2026 = true, vedtakstidspunkt = SporbarUtils.now().minusDays(10)),
        )
        val nyesteBehandling =
            behandlingRepository.insert(
                behandling(fagsakOvergangsstønad)
                    .innvilgetOgFerdigstilt()
                    .copy(vedtakstidspunkt = SporbarUtils.now()),
            )
        lagreAndel(nyesteBehandling, beløp = 5000, tilOgMed = iDagPlusEttÅr())

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.JA)
    }

    @Test
    fun `skal ignorere henlagt behandling og bruke siste reelle behandling på nytt regelverk`() {
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
            InfotrygdPeriodeResponse(
                overgangsstønad = listOf(lagInfotrygdPeriode(personident)),
                barnetilsyn = emptyList(),
                skolepenger = emptyList(),
            )
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        behandlingRepository.insert(
            behandling(fagsakOvergangsstønad)
                .innvilgetOgFerdigstilt()
                .copy(erRegelendring2026 = true, vedtakstidspunkt = SporbarUtils.now().minusDays(5)),
        )
        behandlingRepository.insert(
            behandling(
                fagsak = fagsakOvergangsstønad,
                resultat = BehandlingResultat.HENLAGT,
                status = BehandlingStatus.FERDIGSTILT,
            ).copy(vedtakstidspunkt = SporbarUtils.now()),
        )

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.NEI)
    }

    @Test
    fun `skal returnere JA når siste behandling er på gammelt regelverk selv om perioden er utløpt`() {
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        val behandling = behandlingRepository.insert(behandling(fagsakOvergangsstønad).innvilgetOgFerdigstilt())
        lagreAndel(behandling, beløp = 5000, tilOgMed = LocalDate.now().minusYears(1))

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.JA)
    }

    @Test
    fun `skal returnere JA når siste behandling er på gammelt regelverk selv om beløpet er 0`() {
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        val behandling = behandlingRepository.insert(behandling(fagsakOvergangsstønad).innvilgetOgFerdigstilt())
        lagreAndel(behandling, beløp = 0, tilOgMed = iDagPlusEttÅr())

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.JA)
    }

    @Test
    fun `skal returnere JA når siste behandling er en opphørt sak på gammelt regelverk`() {
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)

        val førsteBehandling = behandlingRepository.insert(behandling(fagsakOvergangsstønad).innvilgetOgFerdigstilt())
        lagreAndel(førsteBehandling, beløp = 5000, tilOgMed = LocalDate.now().minusMonths(6))

        behandlingRepository.insert(
            behandling(fagsakOvergangsstønad).copy(
                resultat = BehandlingResultat.OPPHØRT,
                status = BehandlingStatus.FERDIGSTILT,
                vedtakstidspunkt = SporbarUtils.now(),
            ),
        )

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.JA)
    }

    @Test
    fun `skal returnere NEI når siste behandling er en gammel barnetilsyn`() {
        testoppsettService.lagreFagsak(fagsakBarnetilsyn)
        behandlingRepository.insert(behandling(fagsakBarnetilsyn).innvilgetOgFerdigstilt())

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.NEI)
    }

    @Test
    fun `skal returnere Nei når siste behandling er en gammel skolepenger`() {
        testoppsettService.lagreFagsak(fagsakSkolepenger)
        behandlingRepository.insert(behandling(fagsakSkolepenger).innvilgetOgFerdigstilt())

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.NEI)
    }

    @Test
    fun `skal bruke siste behandling på tvers av stønadsordninger slik at ny overgangsstønad gir NEI selv om eldre barnetilsyn var på gammelt regelverk`() {
        testoppsettService.lagreFagsak(fagsakBarnetilsyn)
        behandlingRepository.insert(
            behandling(fagsakBarnetilsyn)
                .innvilgetOgFerdigstilt()
                .copy(vedtakstidspunkt = SporbarUtils.now().minusDays(10)),
        )
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        behandlingRepository.insert(
            behandling(fagsakOvergangsstønad)
                .innvilgetOgFerdigstilt()
                .copy(erRegelendring2026 = true, vedtakstidspunkt = SporbarUtils.now()),
        )

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.NEI)
    }

    @Test
    fun `skal returnere JA når person kun finnes i Infotrygd`() {
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
            InfotrygdPeriodeResponse(
                overgangsstønad = listOf(lagInfotrygdPeriode(personident)),
                barnetilsyn = emptyList(),
                skolepenger = emptyList(),
            )

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.JA)
    }

    @Test
    fun `skal returnere JA når historisk pensjon har historikk`() {
        every { historiskPensjonClient.hentHistoriskPensjonStatusForIdent(any(), any()) } returns
            HistoriskPensjonDto(HistoriskPensjonStatus.HAR_HISTORIKK, "")

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.JA)
    }

    @Test
    fun `skal returnere VET_IKKE når historisk pensjon er ukjent og ingen treff ellers`() {
        every { historiskPensjonClient.hentHistoriskPensjonStatusForIdent(any(), any()) } returns
            HistoriskPensjonDto(HistoriskPensjonStatus.UKJENT, "")

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.VET_IKKE)
    }

    @Test
    fun `skal returnere VET_IKKE når søker ikke har en aktiv folkeregisteridentifikator`() {
        val historiskIdent =
            Folkeregisteridentifikator(
                ident = personident,
                status = FolkeregisteridentifikatorStatus.I_BRUK,
                metadata = Metadata(historisk = true),
            )
        every { pdlClient.hentSøker(any()) } returns
            PdlClientConfig.opprettPdlSøker().copy(folkeregisteridentifikator = listOf(historiskIdent))

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.VET_IKKE)
    }

    private fun lagreAndel(
        behandling: Behandling,
        beløp: Int,
        tilOgMed: LocalDate,
        fraOgMed: LocalDate = LocalDate.now().minusYears(1),
    ) {
        val andel =
            lagAndelTilkjentYtelse(
                beløp = beløp,
                fraOgMed = fraOgMed,
                tilOgMed = tilOgMed,
                kildeBehandlingId = behandling.id,
            )
        tilkjentYtelseRepository.insert(
            DataGenerator.tilfeldigTilkjentYtelse(behandling).copy(andelerTilkjentYtelse = listOf(andel)),
        )
    }

    private fun iDagPlusEttÅr() = LocalDate.now().plusYears(1)

    private fun hentHarTidligereInnvilgetVedtak() =
        testRestTemplate.exchange<Ressurs<TidligereVedtakStatus>>(
            localhost("/api/ekstern/soknad/har-overgangsstonad-pa-gammelt-regelverk"),
            HttpMethod.GET,
            HttpEntity<Ressurs<TidligereVedtakStatus>>(headers),
        )

    private fun hentHarGyldigBarnetilsynVedRegelendring() =
        testRestTemplate.exchange<Ressurs<Boolean>>(
            localhost("/api/ekstern/soknad/har-gyldig-barnetilsyn-ved-regelendring"),
            HttpMethod.GET,
            HttpEntity<Ressurs<Boolean>>(headers),
        )

    @Test
    fun `skal returnere true når barnetilsyn har løpende andel som dekker 30-06-2026`() {
        testoppsettService.lagreFagsak(fagsakBarnetilsyn)
        val behandling = behandlingRepository.insert(behandling(fagsakBarnetilsyn).innvilgetOgFerdigstilt())
        lagreAndel(
            behandling,
            beløp = 5000,
            fraOgMed = LocalDate.of(2026, 1, 1),
            tilOgMed = LocalDate.of(2026, 12, 31),
        )

        val response = hentHarGyldigBarnetilsynVedRegelendring()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isTrue()
    }

    @Test
    fun `skal returnere false når barnetilsyn ikke har løpende andel som dekker 30-06-2026`() {
        testoppsettService.lagreFagsak(fagsakBarnetilsyn)
        val behandling = behandlingRepository.insert(behandling(fagsakBarnetilsyn).innvilgetOgFerdigstilt())
        lagreAndel(
            behandling,
            beløp = 5000,
            fraOgMed = LocalDate.of(2025, 1, 1),
            tilOgMed = LocalDate.of(2026, 5, 31),
        )

        val response = hentHarGyldigBarnetilsynVedRegelendring()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isFalse()
    }

    @Test
    fun `skal returnere false når det ikke finnes noen barnetilsyn behandling`() {
        val response = hentHarGyldigBarnetilsynVedRegelendring()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isFalse()
    }

    @Test
    fun `skal returnere false når barnetilsyn kun har andel i mars til mai 2026`() {
        testoppsettService.lagreFagsak(fagsakBarnetilsyn)
        val behandling = behandlingRepository.insert(behandling(fagsakBarnetilsyn).innvilgetOgFerdigstilt())
        lagreAndel(
            behandling,
            beløp = 5000,
            fraOgMed = LocalDate.of(2026, 3, 1),
            tilOgMed = LocalDate.of(2026, 5, 31),
        )

        val response = hentHarGyldigBarnetilsynVedRegelendring()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isFalse()
    }
}
