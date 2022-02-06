package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.repository.vedtaksperiode
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class VedtakServiceTest {

    private val vedtakRepository = mockk<VedtakRepository>()
    private val vedtakService = VedtakService(vedtakRepository)
    private val behandlingId = UUID.randomUUID()
    private val inntektsperiodeUtenInntekt = inntektsperiode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), BigDecimal.ZERO)
    private val inntektsperiodeMedInntekt = inntektsperiode(LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31), BigDecimal.valueOf(400000))
    private val inntektWrapper = InntektWrapper(listOf(inntektsperiodeUtenInntekt, inntektsperiodeMedInntekt))

    private val periodeUtenInntekt = vedtaksperiode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
    private val periodeMedInntekt = vedtaksperiode(LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31))
    private val periodeWrapper = PeriodeWrapper(listOf(periodeUtenInntekt, periodeMedInntekt))

    @BeforeEach
    fun setUp() {
        every { vedtakService.hentVedtak(behandlingId) } returns vedtak(behandlingId = behandlingId, inntekter = inntektWrapper, perioder = periodeWrapper)
    }

    @Test
    fun `finn inntekt for tidspunkt`() {

        val ingenAndelInnenGittTidspunkt =
                vedtakService.hentForventetInntektForVedtakOgDato(behandlingId, LocalDate.of(2020, 1, 26))
        Assertions.assertThat(ingenAndelInnenGittTidspunkt).isNull()
        val inntektPaaTidspunktUtenInntekt =
                vedtakService.hentForventetInntektForVedtakOgDato(behandlingId, LocalDate.of(2021, 2, 1))
        Assertions.assertThat(inntektPaaTidspunktUtenInntekt).isEqualTo(0)
        val inntektPaaTidspunktMedInntekt =
                vedtakService.hentForventetInntektForVedtakOgDato(behandlingId, LocalDate.of(2022, 12, 31))
        Assertions.assertThat(inntektPaaTidspunktMedInntekt).isEqualTo(400000)

    }

}