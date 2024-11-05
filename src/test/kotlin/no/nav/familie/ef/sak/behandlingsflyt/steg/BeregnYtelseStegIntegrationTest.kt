package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

internal class BeregnYtelseStegIntegrationTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var beregnYtelseSteg: BeregnYtelseSteg

    @Autowired
    private lateinit var tilkjentytelseRepository: TilkjentYtelseRepository

    private val fagsak = fagsak(fagsakpersoner(setOf("1")))
    private val behandling = behandling(fagsak)
    private val saksbehandling = saksbehandling(fagsak, behandling)
    private val behandling2 = behandling(fagsak, type = BehandlingType.REVURDERING, forrigeBehandlingId = behandling.id)
    private val saksbehandling2 = saksbehandling(fagsak, behandling2)

    private val årMånedFra = YearMonth.of(2021, 1)
    private val årMånedTil = YearMonth.of(2021, 3)

    private val vedtaksperiode = opprettVedtaksperiode(årMånedFra, årMånedTil)
    private val vedtaksperiode2 = opprettVedtaksperiode(årMånedTil, årMånedTil)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    internal fun `kildeBehandlingId skal bli beholdr på andelen som ikke endrer seg`() {
        behandlingRepository.insert(behandling)
        innvilg(saksbehandling, listOf(vedtaksperiode))
        settBehandlingTilIverksatt(behandling)

        behandlingRepository.insert(behandling2)
        innvilg(saksbehandling2, listOf(vedtaksperiode2), listOf(Inntekt(årMånedTil, BigDecimal.ZERO, BigDecimal(10_000))))
        settBehandlingTilIverksatt(behandling2)

        assertThat(hentAndeler(behandling.id)).hasSize(1)
        val andeler = hentAndeler(behandling2.id)
        assertThat(andeler).hasSize(2)
        assertThat(andeler[0].kildeBehandlingId).isEqualTo(behandling.id)
        assertThat(andeler[1].kildeBehandlingId).isEqualTo(behandling2.id)
    }

    @Test
    internal fun `kildeBehandlingId skal bli endret når man skriver over hele perioden`() {
        behandlingRepository.insert(behandling)
        innvilg(saksbehandling, listOf(vedtaksperiode2))
        settBehandlingTilIverksatt(behandling)

        behandlingRepository.insert(behandling2)
        innvilg(
            saksbehandling2,
            listOf(vedtaksperiode),
            listOf(
                Inntekt(årMånedFra, BigDecimal.ZERO, BigDecimal.ZERO),
                Inntekt(årMånedTil, BigDecimal.ZERO, BigDecimal(10_000)),
            ),
        )
        settBehandlingTilIverksatt(behandling2)

        assertThat(hentAndeler(behandling.id)).hasSize(1)
        val andeler = hentAndeler(behandling2.id)
        assertThat(andeler).hasSize(2)
        assertThat(andeler[0].kildeBehandlingId).isEqualTo(behandling2.id)
        assertThat(andeler[1].kildeBehandlingId).isEqualTo(behandling2.id)
    }

    fun settBehandlingTilIverksatt(behandling: Behandling) {
        behandlingRepository.update(
            behandling.copy(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET,
                vedtakstidspunkt = SporbarUtils.now(),
            ),
        )
    }

    private fun hentAndeler(behandlingId: UUID): List<AndelTilkjentYtelse> = tilkjentytelseRepository.findByBehandlingId(behandlingId)!!.andelerTilkjentYtelse.sortedBy { it.stønadFom }

    private fun opprettVedtaksperiode(
        fra: YearMonth,
        til: YearMonth,
    ) = VedtaksperiodeDto(fra, til, Månedsperiode(fra, til), AktivitetType.BARNET_ER_SYKT, VedtaksperiodeType.PERIODE_FØR_FØDSEL)

    private fun innvilg(
        saksbehandling: Saksbehandling,
        vedtaksperioder: List<VedtaksperiodeDto>,
        inntekter: List<Inntekt> = listOf(Inntekt(vedtaksperioder.first().periode.fom, null, null)),
    ) {
        val vedtak =
            InnvilgelseOvergangsstønad(
                perioder = vedtaksperioder,
                inntekter = inntekter,
                periodeBegrunnelse = null,
                inntektBegrunnelse = null,
            )
        beregnYtelseSteg.utførSteg(saksbehandling, vedtak)
    }
}
