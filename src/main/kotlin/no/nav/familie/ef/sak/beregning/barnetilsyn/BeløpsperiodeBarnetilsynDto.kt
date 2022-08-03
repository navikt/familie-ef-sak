package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.kontrakter.felles.Periode
import java.math.BigDecimal
import java.util.UUID

data class BeløpsperiodeBarnetilsynDto(
    val fellesperiode: Periode,
    val periode: no.nav.familie.ef.sak.felles.dto.Periode = no.nav.familie.ef.sak.felles.dto.Periode(
        fellesperiode.fomDato,
        fellesperiode.tomDato
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
