package no.nav.familie.ef.sak.no.nav.familie.ef.sak.metrics

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.metrics.domain.BehandlingerPerStatus
import no.nav.familie.ef.sak.metrics.domain.ForekomsterPerUke
import no.nav.familie.ef.sak.metrics.domain.MålerRepository
import no.nav.familie.ef.sak.metrics.domain.VedtakPerUke
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.temporal.IsoFields

class MålerRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var målerRepository: MålerRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    private val år = LocalDate.now().get(IsoFields.WEEK_BASED_YEAR)
    private val uke = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    @BeforeEach
    fun init() {
        val fagsakBarneTilsyn = fagsak(stønadstype = Stønadstype.BARNETILSYN)
        val fagsakOvergangsstønad = fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD)
        val fagsakSkolepenger = fagsak(stønadstype = Stønadstype.SKOLEPENGER)
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
        val fagsakBarneTilsyn = fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD)

        testoppsettService.lagreFagsak(fagsakBarneTilsyn)

        behandlingRepository.insert(behandling(fagsakBarneTilsyn, årsak = BehandlingÅrsak.MIGRERING))
        assertThat(målerRepository.finnAntallBehandlingerAvÅrsak(BehandlingÅrsak.MIGRERING)).isEqualTo(1)
    }

    @Test
    fun `finnÅpneBehandlinger finner data for åpne behandlinger`() {
        val finnÅpneBehandlinger = målerRepository.finnÅpneBehandlingerPerUke()

        assertThat(finnÅpneBehandlinger.size).isEqualTo(3)
        assertThat(finnÅpneBehandlinger).containsExactlyInAnyOrder(
                ForekomsterPerUke(år, uke, Stønadstype.SKOLEPENGER, 15),
                ForekomsterPerUke(år, uke, Stønadstype.OVERGANGSSTØNAD, 15),
                ForekomsterPerUke(år, uke, Stønadstype.BARNETILSYN, 15)
        )
    }

    @Test
    fun `finnKlarTilBehandling finner antall klar til behandling`() {
        val finnKlarTilBehandling = målerRepository.finnÅpneBehandlinger()

        assertThat(finnKlarTilBehandling.size).isEqualTo(15)
        assertThat(finnKlarTilBehandling).containsExactlyInAnyOrder(
                BehandlingerPerStatus(Stønadstype.SKOLEPENGER, BehandlingStatus.UTREDES, 3),
                BehandlingerPerStatus(Stønadstype.OVERGANGSSTØNAD, BehandlingStatus.UTREDES, 3),
                BehandlingerPerStatus(Stønadstype.BARNETILSYN, BehandlingStatus.UTREDES, 3),
                BehandlingerPerStatus(Stønadstype.OVERGANGSSTØNAD, BehandlingStatus.OPPRETTET, 3),
                BehandlingerPerStatus(Stønadstype.BARNETILSYN, BehandlingStatus.OPPRETTET, 3),
                BehandlingerPerStatus(Stønadstype.SKOLEPENGER, BehandlingStatus.OPPRETTET, 3),
                BehandlingerPerStatus(Stønadstype.OVERGANGSSTØNAD, BehandlingStatus.FATTER_VEDTAK, 3),
                BehandlingerPerStatus(Stønadstype.BARNETILSYN, BehandlingStatus.FATTER_VEDTAK, 3),
                BehandlingerPerStatus(Stønadstype.SKOLEPENGER, BehandlingStatus.FATTER_VEDTAK, 3),
                BehandlingerPerStatus(Stønadstype.OVERGANGSSTØNAD, BehandlingStatus.SATT_PÅ_VENT, 3),
                BehandlingerPerStatus(Stønadstype.BARNETILSYN, BehandlingStatus.SATT_PÅ_VENT, 3),
                BehandlingerPerStatus(Stønadstype.SKOLEPENGER, BehandlingStatus.SATT_PÅ_VENT, 3),
                BehandlingerPerStatus(Stønadstype.OVERGANGSSTØNAD, BehandlingStatus.IVERKSETTER_VEDTAK, 3),
                BehandlingerPerStatus(Stønadstype.BARNETILSYN, BehandlingStatus.IVERKSETTER_VEDTAK, 3),
                BehandlingerPerStatus(Stønadstype.SKOLEPENGER, BehandlingStatus.IVERKSETTER_VEDTAK, 3)
        )
    }

    @Test
    fun `finnVedtak finner data om utførte vedtak`() {
        val finnVedtak = målerRepository.finnVedtakPerUke()

        assertThat(finnVedtak.size).isEqualTo(15)
        assertThat(finnVedtak).containsExactlyInAnyOrder(
                VedtakPerUke(år, uke, Stønadstype.SKOLEPENGER, BehandlingResultat.AVSLÅTT, 3),
                VedtakPerUke(år, uke, Stønadstype.OVERGANGSSTØNAD, BehandlingResultat.AVSLÅTT, 3),
                VedtakPerUke(år, uke, Stønadstype.BARNETILSYN, BehandlingResultat.AVSLÅTT, 3),
                VedtakPerUke(år, uke, Stønadstype.OVERGANGSSTØNAD, BehandlingResultat.HENLAGT, 3),
                VedtakPerUke(år, uke, Stønadstype.BARNETILSYN, BehandlingResultat.HENLAGT, 3),
                VedtakPerUke(år, uke, Stønadstype.SKOLEPENGER, BehandlingResultat.HENLAGT, 3),
                VedtakPerUke(år, uke, Stønadstype.OVERGANGSSTØNAD, BehandlingResultat.INNVILGET, 3),
                VedtakPerUke(år, uke, Stønadstype.BARNETILSYN, BehandlingResultat.INNVILGET, 3),
                VedtakPerUke(år, uke, Stønadstype.SKOLEPENGER, BehandlingResultat.INNVILGET, 3),
                VedtakPerUke(år, uke, Stønadstype.OVERGANGSSTØNAD, BehandlingResultat.OPPHØRT, 3),
                VedtakPerUke(år, uke, Stønadstype.BARNETILSYN, BehandlingResultat.OPPHØRT, 3),
                VedtakPerUke(år, uke, Stønadstype.SKOLEPENGER, BehandlingResultat.OPPHØRT, 3),
                VedtakPerUke(år, uke, Stønadstype.OVERGANGSSTØNAD, BehandlingResultat.IKKE_SATT, 3),
                VedtakPerUke(år, uke, Stønadstype.BARNETILSYN, BehandlingResultat.IKKE_SATT, 3),
                VedtakPerUke(år, uke, Stønadstype.SKOLEPENGER, BehandlingResultat.IKKE_SATT, 3)
        )
    }
}
