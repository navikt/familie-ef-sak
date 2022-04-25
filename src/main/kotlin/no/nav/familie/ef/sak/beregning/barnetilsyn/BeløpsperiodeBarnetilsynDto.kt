package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.felles.dto.Periode
import java.math.BigDecimal
import java.util.UUID

data class BeløpsperiodeBarnetilsynDto(
        val periode: Periode,
        val beløp: Int,
        val beregningsgrunnlag: BeregningsgrunnlagBarnetilsynDto,
)

data class BeregningsgrunnlagBarnetilsynDto(
        val utgifter: BigDecimal,
        val kontantstøttebeløp: BigDecimal,
        val tilleggsstønadsbeløp: BigDecimal,
        val antallBarn: Int,
        val barn: List<UUID>
)