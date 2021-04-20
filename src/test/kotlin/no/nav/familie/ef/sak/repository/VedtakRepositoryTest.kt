package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.beregning.Inntektsperiode
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.repository.*
import no.nav.familie.ef.sak.repository.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

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
                                                                            "aktivitet",
                                                                            "periodeType"))),
                            inntekter = InntektWrapper(listOf(Inntektsperiode(LocalDate.now(),
                                                                              LocalDate.now(),
                                                                              inntekt = BigDecimal(100),
                                                                              samordningFradrag = BigDecimal(0)))))

        vedtakRepository.insert(vedtak)

        assertThat(vedtakRepository.findById(behandling.id)).get().usingRecursiveComparison().isEqualTo(vedtak)
    }
}