package no.nav.familie.ef.sak.fagsak.dto

import no.nav.familie.ef.sak.beregning.Beløpsperiode
import java.time.YearMonth

data class MigreringInfo(
        val kanMigreres: Boolean,
        val årsak: String? = null,
        val stønadFom: YearMonth? = null,
        val stønadTom: YearMonth? = null,
        val inntektsgrunnlag: Int? = null,
        val samordningsfradrag: Int? = null,
        val beløpsperioder: List<Beløpsperiode>? = null
)