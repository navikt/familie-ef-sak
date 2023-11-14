package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import java.time.LocalDate

data class GrunnlagsdataPeriodeHistorikkOvergangsstønad(
    val periodeType: VedtaksperiodeType,
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
)

data class GrunnlagsdataPeriodeHistorikkBarnetilsyn(
    val fom: LocalDate,
    val tom: LocalDate,
)