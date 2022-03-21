package no.nav.familie.ef.sak.no.nav.familie.ef.sak.metrics

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.metrics.domain.BehandlingerPerStatus
import no.nav.familie.ef.sak.metrics.domain.ForekomsterPerUke
import no.nav.familie.ef.sak.metrics.domain.MålerRepository
import no.nav.familie.ef.sak.metrics.domain.VedtakPerUke
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.IsoFields

class MålerRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var målerRepository: MålerRepository
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    private val år = LocalDate.now().get(IsoFields.WEEK_BASED_YEAR)
    private val uke = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    @BeforeEach
    fun init() {
        val fagsakBarneTilsyn = fagsak(stønadstype = StønadType.BARNETILSYN)
        val fagsakOvergangsstønad = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD)
        val fagsakSkolepenger = fagsak(stønadstype = StønadType.SKOLEPENGER)
        val fagsaker = listOf(fagsakBarneTilsyn, fagsakOvergangsstønad, fagsakSkolepenger)

        fagsaker.forEach(testoppsettService::lagreFagsak)

        repeat(3) { // 3 behandlinger
            fagsaker.forEach { fagsak -> // per stønadstype
                BehandlingStatus.values().forEach { status -> // per status
                    if (status == BehandlingStatus.FERDIGSTILT) {
                        BehandlingResultat.values().forEach { resultat -> // per resultat for ferdigstilte
                            behandlingRepository.insert(behandling(fagsak = fagsak, status = status, resultat = resultat))
                        }
                    } else {
                        behandlingRepository.insert(behandling(fagsak = fagsak, status = status))
                    }
                }
            }
        }

    }

    @Test
    internal fun `finnAntallBehandlingerAvÅrsak - finner riktig antall`() {
        assertThat(målerRepository.finnAntallBehandlingerAvÅrsak(BehandlingÅrsak.MIGRERING)).isEqualTo(0)
        val fagsakBarneTilsyn = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD)

        testoppsettService.lagreFagsak(fagsakBarneTilsyn)

        behandlingRepository.insert(behandling(fagsakBarneTilsyn, årsak = BehandlingÅrsak.MIGRERING))
        assertThat(målerRepository.finnAntallBehandlingerAvÅrsak(BehandlingÅrsak.MIGRERING)).isEqualTo(1)
    }

    @Test
    fun `finnÅpneBehandlinger finner data for åpne behandlinger`() {
        val finnÅpneBehandlinger = målerRepository.finnÅpneBehandlingerPerUke()

        assertThat(finnÅpneBehandlinger.size).isEqualTo(3)
        assertThat(finnÅpneBehandlinger).containsExactlyInAnyOrder(
                ForekomsterPerUke(år, uke, StønadType.SKOLEPENGER, 15),
                ForekomsterPerUke(år, uke, StønadType.OVERGANGSSTØNAD, 15),
                ForekomsterPerUke(år, uke, StønadType.BARNETILSYN, 15)
        )
    }

    @Test
    fun `finnKlarTilBehandling finner antall klar til behandling`() {
        val finnKlarTilBehandling = målerRepository.finnÅpneBehandlinger()

        assertThat(finnKlarTilBehandling.size).isEqualTo(15)
        assertThat(finnKlarTilBehandling).containsExactlyInAnyOrder(
                BehandlingerPerStatus(StønadType.SKOLEPENGER, BehandlingStatus.UTREDES, 3),
                BehandlingerPerStatus(StønadType.OVERGANGSSTØNAD, BehandlingStatus.UTREDES, 3),
                BehandlingerPerStatus(StønadType.BARNETILSYN, BehandlingStatus.UTREDES, 3),
                BehandlingerPerStatus(StønadType.OVERGANGSSTØNAD, BehandlingStatus.OPPRETTET, 3),
                BehandlingerPerStatus(StønadType.BARNETILSYN, BehandlingStatus.OPPRETTET, 3),
                BehandlingerPerStatus(StønadType.SKOLEPENGER, BehandlingStatus.OPPRETTET, 3),
                BehandlingerPerStatus(StønadType.OVERGANGSSTØNAD, BehandlingStatus.FATTER_VEDTAK, 3),
                BehandlingerPerStatus(StønadType.BARNETILSYN, BehandlingStatus.FATTER_VEDTAK, 3),
                BehandlingerPerStatus(StønadType.SKOLEPENGER, BehandlingStatus.FATTER_VEDTAK, 3),
                BehandlingerPerStatus(StønadType.OVERGANGSSTØNAD, BehandlingStatus.SATT_PÅ_VENT, 3),
                BehandlingerPerStatus(StønadType.BARNETILSYN, BehandlingStatus.SATT_PÅ_VENT, 3),
                BehandlingerPerStatus(StønadType.SKOLEPENGER, BehandlingStatus.SATT_PÅ_VENT, 3),
                BehandlingerPerStatus(StønadType.OVERGANGSSTØNAD, BehandlingStatus.IVERKSETTER_VEDTAK, 3),
                BehandlingerPerStatus(StønadType.BARNETILSYN, BehandlingStatus.IVERKSETTER_VEDTAK, 3),
                BehandlingerPerStatus(StønadType.SKOLEPENGER, BehandlingStatus.IVERKSETTER_VEDTAK, 3)
        )
    }

    @Test
    fun `finnVedtak finner data om utførte vedtak`() {
        val finnVedtak = målerRepository.finnVedtakPerUke()

        assertThat(finnVedtak.size).isEqualTo(15)
        assertThat(finnVedtak).containsExactlyInAnyOrder(
                VedtakPerUke(år, uke, StønadType.SKOLEPENGER, BehandlingResultat.AVSLÅTT, 3),
                VedtakPerUke(år, uke, StønadType.OVERGANGSSTØNAD, BehandlingResultat.AVSLÅTT, 3),
                VedtakPerUke(år, uke, StønadType.BARNETILSYN, BehandlingResultat.AVSLÅTT, 3),
                VedtakPerUke(år, uke, StønadType.OVERGANGSSTØNAD, BehandlingResultat.HENLAGT, 3),
                VedtakPerUke(år, uke, StønadType.BARNETILSYN, BehandlingResultat.HENLAGT, 3),
                VedtakPerUke(år, uke, StønadType.SKOLEPENGER, BehandlingResultat.HENLAGT, 3),
                VedtakPerUke(år, uke, StønadType.OVERGANGSSTØNAD, BehandlingResultat.INNVILGET, 3),
                VedtakPerUke(år, uke, StønadType.BARNETILSYN, BehandlingResultat.INNVILGET, 3),
                VedtakPerUke(år, uke, StønadType.SKOLEPENGER, BehandlingResultat.INNVILGET, 3),
                VedtakPerUke(år, uke, StønadType.OVERGANGSSTØNAD, BehandlingResultat.OPPHØRT, 3),
                VedtakPerUke(år, uke, StønadType.BARNETILSYN, BehandlingResultat.OPPHØRT, 3),
                VedtakPerUke(år, uke, StønadType.SKOLEPENGER, BehandlingResultat.OPPHØRT, 3),
                VedtakPerUke(år, uke, StønadType.OVERGANGSSTØNAD, BehandlingResultat.IKKE_SATT, 3),
                VedtakPerUke(år, uke, StønadType.BARNETILSYN, BehandlingResultat.IKKE_SATT, 3),
                VedtakPerUke(år, uke, StønadType.SKOLEPENGER, BehandlingResultat.IKKE_SATT, 3)
        )
    }

    @Test
    internal fun `finnAntallLøpendeSaker finner ingen løpende saker når det ikke finnes noen`() {
        val now = YearMonth.now()
        assertThat(målerRepository.finnAntallLøpendeSaker(now.atDay(1), now.plusMonths(1).atEndOfMonth()))
                .isEmpty()
    }

    @Test
    internal fun `skal finne løpende behandlinger`() {
        val now = YearMonth.now()
        val fagsak1 = testoppsettService.lagreFagsak(fagsak(stønadstype = StønadType.OVERGANGSSTØNAD))
        val fagsak2 = testoppsettService.lagreFagsak(fagsak(stønadstype = StønadType.OVERGANGSSTØNAD))
        val behandling1 = opprettFerdigstiltBehandling(fagsak1, LocalDateTime.now().minusDays(1))
        // behandling 2 er gjeldende på fagsak 1 då den er opprettet etter 1
        val behandling2 = opprettFerdigstiltBehandling(fagsak1)
        val behandling1Fagsak2 = opprettFerdigstiltBehandling(fagsak2)

        lagreTilkjentYtelse(behandling1,
                            lagAndelTilkjentYtelse(10_000,
                                                   fraOgMed = now.atDay(1),
                                                   tilOgMed = now.atEndOfMonth()))

        lagreTilkjentYtelse(behandling1, lagAndelTilkjentYtelse(10_000,
                                                                fraOgMed = now.atDay(1),
                                                                tilOgMed = now.atEndOfMonth()))
        lagreTilkjentYtelse(behandling2, lagAndelTilkjentYtelse(2_000,
                                                                fraOgMed = now.atDay(1),
                                                                tilOgMed = now.atEndOfMonth()))
        lagreTilkjentYtelse(behandling1Fagsak2,
                            lagAndelTilkjentYtelse(3_000,
                                                   fraOgMed = now.atDay(1),
                                                   tilOgMed = now.atEndOfMonth()),
                            lagAndelTilkjentYtelse(3_000,
                                                   fraOgMed = now.plusMonths(1).atDay(1),
                                                   tilOgMed = now.plusMonths(1).atEndOfMonth()),
                            lagAndelTilkjentYtelse(20_000,
                                                   fraOgMed = now.plusMonths(3).atDay(1),
                                                   tilOgMed = now.plusMonths(3).atEndOfMonth()))

        val løpendeSaker = målerRepository.finnAntallLøpendeSaker(now.atDay(1), now.plusMonths(1).atDay(1))
        assertThat(løpendeSaker).hasSize(2)
        val denneMåned = løpendeSaker.single { it.dato == now.atDay(1) }
        val nesteMåned = løpendeSaker.single { it.dato == now.plusMonths(1).atDay(1) }
        assertThat(denneMåned.antall).isEqualTo(2)
        assertThat(denneMåned.belop).isEqualTo(5_000)
        assertThat(nesteMåned.antall).isEqualTo(1)
        assertThat(nesteMåned.belop).isEqualTo(3000)

    }

    private fun opprettFerdigstiltBehandling(fagsak: Fagsak, opprettetTid: LocalDateTime = LocalDateTime.now()) =
            behandlingRepository.insert(behandling(fagsak,
                                                   status = BehandlingStatus.FERDIGSTILT,
                                                   resultat = BehandlingResultat.INNVILGET,
                                                   opprettetTid = opprettetTid))

    private fun lagreTilkjentYtelse(behandling: Behandling, vararg andelTilkjentYtelse: AndelTilkjentYtelse) {
        val andeler = andelTilkjentYtelse.map { it.copy(kildeBehandlingId = behandling.id) }.toList()
        tilkjentYtelseRepository.insert(lagTilkjentYtelse(andeler, behandlingId = behandling.id))
    }
}
