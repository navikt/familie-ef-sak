package no.nav.familie.ef.sak.beregning.barnetilsyn

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.kontrakter.felles.Datoperiode
import java.math.BigDecimal
import java.util.UUID

data class BeløpsperiodeBarnetilsynDto(
    @JsonProperty("fellesperiode")
    val periode: Datoperiode,
    @Deprecated("Bruk periode", ReplaceWith("periode"))
    @JsonProperty("periode")
    val deprecatedPeriode: no.nav.familie.ef.sak.felles.dto.Periode = no.nav.familie.ef.sak.felles.dto.Periode(
        periode.fom,
        periode.tom
    ),
    val beløp: Int,
    val beløpFørFratrekkOgSatsjustering: Int,
    val sats: Int,
    val beregningsgrunnlag: BeregningsgrunnlagBarnetilsynDto,
)

data class BeregningsgrunnlagBarnetilsynDto(
    val utgifter: BigDecimal,
    val kontantstøttebeløp: BigDecimal,
    val tilleggsstønadsbeløp: BigDecimal,
    val antallBarn: Int,
    val barn: List<UUID>
)
