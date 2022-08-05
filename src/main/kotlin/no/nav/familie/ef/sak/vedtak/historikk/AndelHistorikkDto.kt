package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Datoperiode
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
    val andel: AndelMedGrunnlagDto,
    val aktivitet: AktivitetType?,
    val aktivitetArbeid: SvarId?,
    val periodeType: VedtaksperiodeType?,
    val erSanksjon: Boolean,
    val sanksjonsårsak: Sanksjonsårsak?,
    val endring: HistorikkEndring?
)

fun AndelHistorikkDto.erIkkeFjernet() =
    this.endring?.type == null || this.endring.type == EndringType.SPLITTET

data class AndelMedGrunnlagDto(
    val beløp: Int,
    @Deprecated("Bruk periode!", ReplaceWith("periode.fomDato")) val stønadFra: LocalDate?,
    @Deprecated("Bruk periode!", ReplaceWith("periode.tomDato")) val stønadTil: LocalDate?,
    val periode: Datoperiode = Datoperiode(
        stønadFra ?: error("Periode eller stønadFra må ha verdi"),
        stønadTil ?: error("Periode eller stønadTil må ha verdi")
    ),
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
        vedtaksinformasjon: VedtakshistorikkperiodeBarnetilsyn?
    ) : this(
        beløp = andel.beløp,
        stønadFra = andel.stønadFom,
        stønadTil = andel.stønadTom,
        periode = andel.periode.toDatoperiode(),
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
}

data class HistorikkEndring(
    val type: EndringType,
    val behandlingId: UUID,
    val vedtakstidspunkt: LocalDateTime
)
