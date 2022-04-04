package no.nav.familie.ef.sak.beregning.barnetilsyndto

import no.nav.familie.ef.sak.felles.dto.Periode
import java.math.BigDecimal

data class BeløpsperiodeBarnetilsynDto(
        val periode: Periode,
        val beløp: BigDecimal,
        val beregningsgrunnlag: BeregningsgrunnlagBarnetilsynDto,
)

data class BeregningsgrunnlagBarnetilsynDto(
        val utgiftsbeløp: BigDecimal,
        val kontantstøttebeløp: BigDecimal,
        val tilleggsstønadsbeløp: BigDecimal,
        val antallBarn: Int
)