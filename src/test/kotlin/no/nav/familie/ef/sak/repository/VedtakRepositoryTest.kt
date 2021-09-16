package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.vedtak.AktivitetType
import no.nav.familie.ef.sak.vedtak.InntektWrapper
import no.nav.familie.ef.sak.vedtak.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.ResultatType
import no.nav.familie.ef.sak.vedtak.Vedtak
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.VedtaksperiodeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

internal class VedtakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var vedtakRepository: VedtakRepository
    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun `skal lagre vedtak med riktige felter`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vedtak = Vedtak(behandlingId = behandling.id,
                            resultatType = ResultatType.INNVILGE,
                            periodeBegrunnelse = "begrunnelse for periode",
                            inntektBegrunnelse = "begrunnelse for inntekt",
                            perioder = PeriodeWrapper(listOf(Vedtaksperiode(LocalDate.now(),
                                                                            LocalDate.now(),
                                                                            AktivitetType.FORSØRGER_ETABLERER_VIRKSOMHET,
                                                                            VedtaksperiodeType.HOVEDPERIODE))),
                            inntekter = InntektWrapper(listOf(Inntektsperiode(LocalDate.now(),
                                                                              LocalDate.now(),
                                                                              inntekt = BigDecimal(100),
                                                                              samordningsfradrag = BigDecimal(0)))))

        vedtakRepository.insert(vedtak)

        assertThat(vedtakRepository.findById(behandling.id)).get().usingRecursiveComparison().isEqualTo(vedtak)
    }
}