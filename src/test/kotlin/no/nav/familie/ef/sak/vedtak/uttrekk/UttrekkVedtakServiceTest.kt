package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType.BARNET_ER_SYKT
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.YearMonth

internal class UttrekkVedtakServiceTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var beregnYtelseSteg: BeregnYtelseSteg
    @Autowired private lateinit var tilkjentytelseRepository: TilkjentYtelseRepository
    @Autowired private lateinit var uttrekkVedtakService: UttrekkVedtakService
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private val fagsak = fagsak(fagsakpersoner(setOf("1")))
    private val behandling = behandling(fagsak)
    private val behandling2 = behandling(fagsak, type = BehandlingType.REVURDERING, forrigeBehandlingId = behandling.id)

    private val januar2021 = YearMonth.of(2021, 1)
    private val februar2021 = YearMonth.of(2021, 2)
    private val mars2021 = YearMonth.of(2021, 3)

    private val vedtaksperiode = opprettVedtaksperiode(januar2021, mars2021)
    private val vedtaksperiode2 = opprettVedtaksperiode(februar2021, februar2021,
                                                        aktivitetType = BARNET_ER_SYKT)
    private val vedtaksperiode3 = opprettVedtaksperiode(mars2021, mars2021,
                                                        aktivitetType = FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER)

    @Test
    internal fun `skal kjøre query uten problemer`() {
        assertThat(uttrekkVedtakService.hentArbeidssøkere()).isEmpty()
    }

    @Test
    internal fun `skal ikke finne andre aktivitettyper enn de som søker etter arbeid`() {
        opprettdata()
        val arbeidssøkere = uttrekkVedtakService.hentArbeidssøkere(februar2021)
        assertThat(arbeidssøkere).isEmpty()
    }

    @Test
    internal fun `behandlingIdForVedtak skal peke til behandlingen der vedtaket ble opprettet`() {
        opprettdata()

        val arbeidssøkereJan = uttrekkVedtakService.hentArbeidssøkere(januar2021)
        assertThat(arbeidssøkereJan).hasSize(1)
        assertThat(arbeidssøkereJan[0].behandlingId).isEqualTo(behandling2.id)
        assertThat(arbeidssøkereJan[0].behandlingIdForVedtak).isEqualTo(behandling.id)

        val arbeidssøkereMars = uttrekkVedtakService.hentArbeidssøkere(mars2021)
        assertThat(arbeidssøkereMars).hasSize(1)
        assertThat(arbeidssøkereMars[0].behandlingId).isEqualTo(behandling2.id)
        assertThat(arbeidssøkereMars[0].behandlingIdForVedtak).isEqualTo(behandling2.id)
    }

    private fun opprettdata() {
        opprettBehandlinger()
        innvilg(behandling, listOf(vedtaksperiode))
        ferdigstillBehandling(behandling)
        innvilg(behandling2,
                listOf(vedtaksperiode2, vedtaksperiode3),
                listOf(Inntekt(februar2021, BigDecimal.ZERO, BigDecimal(10_000))))
        ferdigstillBehandling(behandling2)
    }

    fun ferdigstillBehandling(behandling: Behandling) {
        behandlingRepository.update(behandling.copy(status = BehandlingStatus.FERDIGSTILT,
                                                    resultat = BehandlingResultat.INNVILGET))
    }

    private fun opprettVedtaksperiode(fra: YearMonth,
                                      til: YearMonth,
                                      aktivitetType: AktivitetType = AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER) =
            VedtaksperiodeDto(fra, til, aktivitetType, VedtaksperiodeType.PERIODE_FØR_FØDSEL)

    private fun innvilg(behandling: Behandling,
                        vedtaksperioder: List<VedtaksperiodeDto>,
                        inntekter: List<Inntekt> = listOf(Inntekt(vedtaksperioder.first().årMånedFra, null, null))) {
        val vedtak = Innvilget(perioder = vedtaksperioder,
                               inntekter = inntekter,
                               periodeBegrunnelse = null,
                               inntektBegrunnelse = null)
        beregnYtelseSteg.utførSteg(behandling, vedtak)
    }

    fun opprettBehandlinger() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
        behandlingRepository.insert(behandling2)
    }
}