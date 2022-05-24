package no.nav.familie.ef.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.repository.vedtaksperiode
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
    private val inntektsperiodeUtenInntekt = inntektsperiode(startDato = LocalDate.of(2021, 1, 1),
                                                             sluttDato = LocalDate.of(2021, 12, 31),
                                                             inntekt = BigDecimal.ZERO)
    private val inntektsperiodeMedInntekt = inntektsperiode(startDato = LocalDate.of(2022, 1, 1),
                                                            sluttDato = LocalDate.of(2024, 12, 31),
                                                            inntekt = BigDecimal.valueOf(400000))
    private val inntektWrapper = InntektWrapper(listOf(inntektsperiodeUtenInntekt, inntektsperiodeMedInntekt))

    private val periodeUtenInntekt = vedtaksperiode(startDato = LocalDate.of(2021, 1, 1),
                                                    sluttDato = LocalDate.of(2021, 12, 31))
    private val periodeMedInntekt = vedtaksperiode(startDato = LocalDate.of(2022, 1, 1),
                                                   sluttDato = LocalDate.of(2024, 12, 31))
    private val periodeWrapper = PeriodeWrapper(listOf(periodeUtenInntekt, periodeMedInntekt))

    @BeforeEach
    fun setUp() {
        every { vedtakService.hentVedtak(behandlingId) } returns vedtak(behandlingId = behandlingId,
                                                                        inntekter = inntektWrapper,
                                                                        perioder = periodeWrapper)
    }

    @Test
    fun `finn inntekt for tidspunkt`() {

        val ingenAndelInnenGittTidspunkt =
                vedtakService.hentForventetInntektForBehandlingIds(behandlingId, LocalDate.of(2020, 1, 26))
        Assertions.assertThat(ingenAndelInnenGittTidspunkt).isNull()
        val inntektPaaTidspunktUtenInntekt =
                vedtakService.hentForventetInntektForBehandlingIds(behandlingId, LocalDate.of(2021, 2, 1))
        Assertions.assertThat(inntektPaaTidspunktUtenInntekt).isEqualTo(0)
        val inntektPaaTidspunktMedInntekt =
                vedtakService.hentForventetInntektForBehandlingIds(behandlingId, LocalDate.of(2022, 12, 31))
        Assertions.assertThat(inntektPaaTidspunktMedInntekt).isEqualTo(400000)

    }

}