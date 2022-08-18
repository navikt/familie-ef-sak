package no.nav.familie.ef.sak.tilkjentytelse

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TilkjentYtelseDto(
    val behandlingId: UUID,
    val vedtakstidspunkt: LocalDateTime,
    val andeler: List<AndelTilkjentYtelseDto>,
    val samordningsfradragType: SamordningsfradragType?
)

data class AndelTilkjentYtelseDto(
    val beløp: Int,
    val periode: Månedsperiode,
    val inntekt: Int,
    val inntektsreduksjon: Int,
    val samordningsfradrag: Int
) {
    @Deprecated("Bruk periode.", ReplaceWith("periode.fomDato"))
    @get:JsonProperty
    val stønadFra: LocalDate get() = periode.fomDato

    @Deprecated("Bruk periode.", ReplaceWith("periode.tomDato"))
    @get:JsonProperty
    val stønadTil: LocalDate get() = periode.tomDato
}

data class BarnMedLøpendeStønad(val barn: List<UUID>, val dato: LocalDate)
