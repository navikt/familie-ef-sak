package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeSkolepengerDto

data class BeregningSkolepengerRequest(
    val utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>,
)

data class BeløpsperiodeSkolepengerDto(
    val periode: Periode,
    val beløp: Int,
    val beregningsgrunnlag: BeregningsgrunnlagSkolepengerDto,
)

data class BeregningsgrunnlagSkolepengerDto(
    val studietype: SkolepengerStudietype,
    val utgifter: Int,
    val studiebelastning: Int,
)
