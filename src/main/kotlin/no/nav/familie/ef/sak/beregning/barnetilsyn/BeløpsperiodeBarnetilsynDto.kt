package no.nav.familie.ef.sak.beregning.barnetilsyn

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.util.UUID

data class BeløpsperiodeBarnetilsynDto(
    @JsonProperty("fellesperiode")
    val periode: Månedsperiode,
    val beløp: Int,
    val beløpFørFratrekkOgSatsjustering: Int,
    val sats: Int,
    val beregningsgrunnlag: BeregningsgrunnlagBarnetilsynDto,
    val periodetype: PeriodetypeBarnetilsyn,
    val aktivitetstype: AktivitetstypeBarnetilsyn?
) {
    @Deprecated("Bruk periode", ReplaceWith("periode"))
    @get:JsonProperty("periode")
    val deprecatedPeriode: no.nav.familie.ef.sak.felles.dto.Periode
        get() =
            no.nav.familie.ef.sak.felles.dto.Periode(periode.fomDato, periode.tomDato)
}

data class BeregningsgrunnlagBarnetilsynDto(
    val utgifter: BigDecimal,
    val kontantstøttebeløp: BigDecimal,
    val tilleggsstønadsbeløp: BigDecimal,
    val antallBarn: Int,
    val barn: List<UUID>
)
