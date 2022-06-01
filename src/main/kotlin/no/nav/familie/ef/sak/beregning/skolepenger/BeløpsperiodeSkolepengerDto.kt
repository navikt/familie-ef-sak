package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.dto.SkolepengerUtgiftDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeSkolepengerDto
import java.time.Year

data class BeregningSkolepengerRequest(
    val utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>,
)

data class BeløpsperiodeSkolepengerDto(
    val skoleår: Year,
    val perioder: List<BeløpSkolepenger>
)

data class BeløpSkolepenger(
    val maksbeløp: Int,
    val maksbeløpFordeltAntallMåneder: Int,
    val tidligereForbrukt: Int,
    val nyForbrukt: Int,
    val grunnlag: BeregningsgrunnlagSkolepengerDto,
    val nyeUtbetalinger: List<DetaljertBeløpSkolepenger>
)

data class BeregningsgrunnlagSkolepengerDto(
    val studietype: SkolepengerStudietype,
    val studiebelastning: Int,
    val periode: Periode,
)

data class DetaljertBeløpSkolepenger(
    val stønad: Int,
    val grunnlag: SkolepengerUtgiftDto
)
