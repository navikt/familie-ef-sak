package no.nav.familie.ef.sak.vedtak.historikk

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class EndringType {
    FJERNET,
    ERSTATTET,
    SPLITTET,
}

data class AndelHistorikkDto(
    val behandlingId: UUID,
    val behandlingType: BehandlingType,
    val behandlingÅrsak: BehandlingÅrsak,
    val vedtakstidspunkt: LocalDateTime,
    val saksbehandler: String,
    val vedtaksperiode: Vedtakshistorikkperiode,
    val andel: AndelMedGrunnlagDto,
    val aktivitet: AktivitetType?, // finnes i vedtaksperiode
    val aktivitetBarnetilsyn: AktivitetstypeBarnetilsyn?, // finnes i vedtaksperiode
    val aktivitetArbeid: SvarId?,
    val periodeType: VedtaksperiodeType?, // finnes i vedtaksperiode
    val periodetypeBarnetilsyn: PeriodetypeBarnetilsyn?, // finnes i vedtaksperiode
    val erSanksjon: Boolean, // TODO denne kan fjernes / flyttes som en get og være beroende av periodetype / periodetypeBarnetilsyn
    val sanksjonsårsak: Sanksjonsårsak?, // finnes i vedtaksperiode
    val erOpphør: Boolean,
    val endring: HistorikkEndring?,
)

/**
 * AndelHistorikk kan inneholde andeler som er fjernet eller erstatte,
 * disse skal ikke tas med når man skal plukke ut alle aktive andelene
 */
fun AndelHistorikkDto.erAktivVedtaksperiode() = !erOpphør && (this.endring?.type == null || this.endring.type == EndringType.SPLITTET)

data class AndelMedGrunnlagDto(
    val beløp: Int,
    val periode: Månedsperiode,
    val inntekt: Int,
    val inntektsreduksjon: Int,
    val samordningsfradrag: Int,
    val kontantstøtte: Int,
    val tilleggsstønad: Int,
    val antallBarn: Int,
    val utgifter: BigDecimal = BigDecimal.ZERO,
    val barn: List<UUID>,
    val sats: Int,
    val beløpFørFratrekkOgSatsJustering: Int,
) {
    constructor(
        andel: AndelTilkjentYtelse,
        vedtaksinformasjon: VedtakshistorikkperiodeBarnetilsyn?,
    ) : this(
        beløp = andel.beløp,
        periode = andel.periode,
        inntekt = andel.inntekt,
        inntektsreduksjon = andel.inntektsreduksjon,
        samordningsfradrag = andel.samordningsfradrag,
        kontantstøtte = vedtaksinformasjon?.kontantstøtte ?: 0,
        tilleggsstønad = vedtaksinformasjon?.tilleggsstønad ?: 0,
        utgifter = vedtaksinformasjon?.utgifter ?: BigDecimal.ZERO,
        antallBarn = vedtaksinformasjon?.antallBarn ?: 0,
        barn = vedtaksinformasjon?.barn ?: emptyList(),
        sats = vedtaksinformasjon?.sats ?: 0,
        beløpFørFratrekkOgSatsJustering = vedtaksinformasjon?.beløpFørFratrekkOgSatsjustering ?: 0,
    )

    @Deprecated("Bruk periode!", ReplaceWith("periode.fomDato"))
    @get:JsonProperty
    val stønadFra: LocalDate get() = periode.fomDato

    @Deprecated("Bruk periode!", ReplaceWith("periode.tomDato"))
    @get:JsonProperty
    val stønadTil: LocalDate get() = periode.tomDato

    @get:JsonProperty
    val beregnetAntallMåneder: Int get() = AndelHistorikkBeregner.regnUtAntallMåneder(periode)
}

data class HistorikkEndring(
    val type: EndringType,
    val behandlingId: UUID,
    val vedtakstidspunkt: LocalDateTime,
)
