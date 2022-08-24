package no.nav.familie.ef.sak.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.UUID

internal class BeregningControllerUnitTest {

    val tilkjentytelseService = mockk<TilkjentYtelseService>()
    val vedtakService = mockk<VedtakService>()

    val beregningController = BeregningController(
        beregningService = BeregningService(),
        tilgangService = mockk(relaxed = true),
        tilkjentYtelseService = tilkjentytelseService,
        vedtakService = vedtakService
    )

    @Test
    internal fun `skal kaste feil dersom vedtak har resultattypen OPPHØRT`() {

        every { vedtakService.hentVedtak(any()) } returns
            Vedtak(
                behandlingId = UUID.randomUUID(),
                resultatType = ResultatType.OPPHØRT,
                perioder =
                PeriodeWrapper(
                    perioder = listOf(
                        Vedtaksperiode(
                            periode = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 4)),
                            aktivitet = AktivitetType.BARN_UNDER_ETT_ÅR,
                            periodeType = VedtaksperiodeType.MIDLERTIDIG_OPPHØR
                        )
                    )
                )
            )
        every { tilkjentytelseService.hentForBehandling(any()) } returns
            lagTilkjentYtelse(
                andelerTilkjentYtelse = listOf(
                    lagAndelTilkjentYtelse(
                        fraOgMed = YearMonth.of(2022, 1),
                        beløp = 10_000,
                        tilOgMed = YearMonth.of(2022, 4),
                    )
                )
            )
        assertThrows<Feil> { beregningController.hentBeregnetBeløp(UUID.randomUUID()) }
    }
}
