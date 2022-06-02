package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.dto.SkolepengerUtgiftDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeSkolepengerDto
import java.time.Year

data class BeregningSkolepengerRequest(
    val utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>,
)

data class BeregningSkolepengerResponse(
    val perioder: List<BeløpsperiodeSkolepenger>
)

data class BeløpsperiodeSkolepenger(
    val skoleår: Year,
    val maksbeløp: Int,
    val maksbeløpFordeltAntallMåneder: Int,
    val alleredeUtbetalt: Int,
    val nyForbrukt: Int, // ?
    val grunnlag: BeregningsgrunnlagSkolepengerDto,
    val utbetalinger: List<BeregnetUtbetalingSkolepenger>
)

data class BeregningsgrunnlagSkolepengerDto(
    val studietype: SkolepengerStudietype,
    val studiebelastning: Int,
    val periode: Periode,
)

data class BeregnetUtbetalingSkolepenger(
    val beløp: Int,
    val grunnlag: SkolepengerUtgiftDto
)
