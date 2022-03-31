package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth

internal class StegServiceTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var stegService: StegService
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository
    @Autowired lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun `skal håndtere en ny søknad`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandling(fagsak, status = BehandlingStatus.UTREDES)
        behandlingRepository.insert(behandling)
        stegService.håndterVilkår(saksbehandling(fagsak, behandling))
    }

    @Test
    internal fun `skal legge inn historikkinnslag for beregn ytelse selv om behandlingen står på send til beslutter`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("0101017227"))))
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                status = BehandlingStatus.UTREDES,
                                                                steg = StegType.SEND_TIL_BESLUTTER))

        val vedtaksperiode = VedtaksperiodeDto(årMånedFra = YearMonth.of(2021, 1),
                                               årMånedTil = YearMonth.of(2021, 6),
                                               aktivitet = AktivitetType.BARN_UNDER_ETT_ÅR,
                                               periodeType = VedtaksperiodeType.HOVEDPERIODE)
        val inntek = Inntekt(årMånedFra = YearMonth.of(2021, 1),
                             forventetInntekt = BigDecimal(12345),
                             samordningsfradrag = BigDecimal(2))
        stegService.håndterBeregnYtelseForStønad(saksbehandling(fagsak, behandling),
                                                 vedtak = InnvilgelseOvergangsstønad(periodeBegrunnelse = "ok",
                                                                                     inntektBegrunnelse = "okok",
                                                                                     perioder = listOf(vedtaksperiode),
                                                                                     inntekter = listOf(inntek),
                                                                                     samordningsfradragType = SamordningsfradragType.UFØRETRYGD))

        assertThat(behandlingshistorikkRepository.findByBehandlingIdOrderByEndretTidDesc(behandling.id).first().steg)
                .isEqualTo(StegType.BEREGNE_YTELSE)

    }

    @Test
    internal fun `skal feile håndtering av ny søknad hvis en behandling er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.BEHANDLING_FERDIGSTILT))

        assertThrows<IllegalStateException> {
            stegService.håndterVilkår(saksbehandling(fagsak, behandling))
        }
    }

    @Test
    internal fun `skal feile håndtering av ny søknad hvis en behandling er sendt til beslutter`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.BESLUTTE_VEDTAK))

        assertThrows<IllegalStateException> {
            stegService.håndterVilkår(saksbehandling(fagsak, behandling))
        }
    }
}