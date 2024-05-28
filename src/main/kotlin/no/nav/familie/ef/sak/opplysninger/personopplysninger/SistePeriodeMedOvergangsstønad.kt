package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import java.time.LocalDate

data class SistePeriodeMedOvergangsstønad(
    val fom: LocalDate,
    val tom: LocalDate,
    val periodeType: VedtaksperiodeType,
    val inntekt: Int?,
    val samordningsfradrag: Int?,
)
