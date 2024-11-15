package no.nav.familie.ef.sak.fagsak.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.sak.beregning.Beløpsperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.YearMonth

data class MigreringInfo(
    val kanMigreres: Boolean,
    val årsak: String? = null,
    val stønadsperiode: Månedsperiode? = null,
    val inntektsgrunnlag: Int? = null,
    val samordningsfradrag: Int? = null,
    val beløpsperioder: List<Beløpsperiode>? = null,
) {
    @Deprecated("Bruk stønadsperiode.", ReplaceWith("stønadsperiode.fomMåned"))
    @get:JsonProperty
    val stønadFom: YearMonth? get() = stønadsperiode?.fom

    @Deprecated("Bruk stønadsperiode.", ReplaceWith("stønadsperiode.tomMåned"))
    @get:JsonProperty
    val stønadTom: YearMonth? get() = stønadsperiode?.tom
}

data class MigrerRequestDto(
    val ignorerFeilISimulering: Boolean = false,
)
