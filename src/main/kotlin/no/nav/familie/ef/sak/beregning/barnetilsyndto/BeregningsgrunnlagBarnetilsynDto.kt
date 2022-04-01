package no.nav.familie.ef.sak.beregning.barnetilsyndto

import no.nav.familie.ef.sak.beregning.KontantstøttePeriodeDto
import no.nav.familie.ef.sak.beregning.TilleggsstønadPeriodeDto
import no.nav.familie.ef.sak.beregning.UtgiftsperiodeDto

data class BeregningsgrunnlagBarnetilsynDto(
        val utgiftsperioder: List<UtgiftsperiodeDto>,
        val kontantstøtteperioder: List<KontantstøttePeriodeDto>,
        val tilleggsstønadsperioder: List<TilleggsstønadPeriodeDto>
)

