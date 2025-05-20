package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.steg.VilkårSteg
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth

internal class StegServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var stegService: StegService

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vedtakService: VedtakService

    @Autowired
    lateinit var vilkårSteg: VilkårSteg

    @Autowired
    lateinit var beregnYtelseSteg: BeregnYtelseSteg

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal håndtere en ny søknad`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandling(fagsak, status = BehandlingStatus.UTREDES)
        behandlingRepository.insert(behandling)
        stegService.håndterSteg(saksbehandling(fagsak, behandling), vilkårSteg, null)
    }

    @Test
    internal fun `skal feile håndtering av ny søknad hvis en behandling er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.BEHANDLING_FERDIGSTILT))

        assertThrows<IllegalStateException> {
            stegService.håndterSteg(saksbehandling(fagsak, behandling), vilkårSteg, null)
        }
    }

    @Test
    internal fun `skal feile håndtering av ny søknad hvis en behandling er sendt til beslutter`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.BESLUTTE_VEDTAK))

        assertThrows<IllegalStateException> {
            stegService.håndterSteg(saksbehandling(fagsak, behandling), vilkårSteg, null)
        }
    }

    @Test
    internal fun `skal legge inn historikkinnslag for beregn ytelse selv om behandlingen står på send til beslutter`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("0101017227"))))
        val behandling =
            behandlingRepository.insert(
                behandling(
                    fagsak,
                    status = BehandlingStatus.UTREDES,
                    steg = StegType.SEND_TIL_BESLUTTER,
                ),
            )

        val vedtaksperiode =
            VedtaksperiodeDto(
                årMånedFra = YearMonth.of(2021, 1),
                årMånedTil = YearMonth.of(2021, 6),
                periode = Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 6)),
                aktivitet = AktivitetType.BARN_UNDER_ETT_ÅR,
                periodeType = VedtaksperiodeType.HOVEDPERIODE,
            )
        val inntek =
            Inntekt(
                årMånedFra = YearMonth.of(2021, 1),
                forventetInntekt = BigDecimal(12345),
                samordningsfradrag = BigDecimal(2),
            )
        stegService.håndterSteg(
            saksbehandling(fagsak, behandling),
            beregnYtelseSteg,
            InnvilgelseOvergangsstønad(
                periodeBegrunnelse = "ok",
                inntektBegrunnelse = "okok",
                perioder = listOf(vedtaksperiode),
                inntekter = listOf(inntek),
                samordningsfradragType = SamordningsfradragType.UFØRETRYGD,
            ),
        )

        assertThat(behandlingshistorikkRepository.findByBehandlingIdOrderByEndretTidDesc(behandling.id).first().steg)
            .isEqualTo(StegType.BEREGNE_YTELSE)
    }
}
