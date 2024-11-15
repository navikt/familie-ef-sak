package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class AndelHistorikkDtoKtTest {
    @Test
    internal fun `andel er aktiv hvis perioden ikke har endring eller er splittet`() {
        assertThat(andel.erAktivVedtaksperiode()).isTrue
        assertThat(andel.medEndring(EndringType.SPLITTET).erAktivVedtaksperiode()).isTrue
    }

    @Test
    internal fun `andel er ikke aktiv hvis den er fjernet eller erstattet`() {
        assertThat(andel.medEndring(EndringType.FJERNET).erAktivVedtaksperiode()).isFalse
        assertThat(andel.medEndring(EndringType.ERSTATTET).erAktivVedtaksperiode()).isFalse
    }

    @Test
    internal fun `andel er ikke aktiv hvis den inneholder opphør`() {
        assertThat(andel.medOpphør().erAktivVedtaksperiode()).isFalse
        assertThat(andel.medOpphør().medEndring(EndringType.FJERNET).erAktivVedtaksperiode()).isFalse
        assertThat(andel.medOpphør().medEndring(EndringType.SPLITTET).erAktivVedtaksperiode()).isFalse
    }

    @Test
    internal fun `beregnetAntallMåneder skal returnere antall måneder mellom to datoer`() {
        val andelMedGrunnlagDtoNyPeriode = andelMedGrunnlagDto().copy(periode = Månedsperiode(fom = YearMonth.now().minusMonths(3), tom = YearMonth.now()))
        val beregnetAntallMåneder = andelMedGrunnlagDtoNyPeriode.beregnetAntallMåneder

        val andelMedGrunnlagDtoNyPeriode2 = andelMedGrunnlagDto().copy(periode = Månedsperiode(fom = YearMonth.now(), tom = YearMonth.now()))
        val beregnetAntallMåneder2 = andelMedGrunnlagDtoNyPeriode2.beregnetAntallMåneder

        assertThat(beregnetAntallMåneder).isEqualTo(4)
        assertThat(beregnetAntallMåneder2).isEqualTo(1)
    }

    private fun AndelHistorikkDto.medOpphør() = this.copy(erOpphør = true)

    private fun AndelHistorikkDto.medEndring(endringType: EndringType) = this.copy(endring = HistorikkEndring(endringType, UUID.randomUUID(), LocalDateTime.now()))

    val andel =
        AndelHistorikkDto(
            behandlingId = UUID.randomUUID(),
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            vedtakstidspunkt = LocalDateTime.now(),
            saksbehandler = "",
            vedtaksperiode =
                VedtakshistorikkperiodeOvergangsstønad(
                    periode = Månedsperiode(YearMonth.now()),
                    aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
                    periodeType = VedtaksperiodeType.HOVEDPERIODE,
                    inntekt = Inntekt(YearMonth.now(), BigDecimal.ZERO, BigDecimal.ZERO),
                ),
            andel = andelMedGrunnlagDto(),
            aktivitet = null,
            aktivitetArbeid = null,
            periodeType = null,
            erSanksjon = false,
            sanksjonsårsak = null,
            erOpphør = false,
            periodetypeBarnetilsyn = PeriodetypeBarnetilsyn.ORDINÆR,
            aktivitetBarnetilsyn = AktivitetstypeBarnetilsyn.I_ARBEID,
            endring = null,
        )

    private fun andelMedGrunnlagDto() =
        AndelMedGrunnlagDto(
            beløp = 0,
            periode = Månedsperiode(YearMonth.now()),
            inntekt = 0,
            inntektsreduksjon = 0,
            samordningsfradrag = 0,
            kontantstøtte = 0,
            tilleggsstønad = 0,
            antallBarn = 0,
            barn = emptyList(),
            sats = 0,
            beløpFørFratrekkOgSatsJustering = 0,
        )
}
