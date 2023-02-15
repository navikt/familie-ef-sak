package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.vedtak.dto.SkoleårsperiodeSkolepengerDto
import java.time.YearMonth
import java.util.UUID

data class BeregningSkolepengerRequest(
    val behandlingId: UUID,
    val skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>,
    val erOpphør: Boolean = false,
)

data class BeregningSkolepengerResponse(
    val perioder: List<BeløpsperiodeSkolepenger>,
)

data class BeløpsperiodeSkolepenger(
    val årMånedFra: YearMonth,
    val utgifter: Int,
    val beløp: Int,
)
