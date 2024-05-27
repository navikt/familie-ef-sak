package no.nav.familie.ef.sak.opplysninger.personopplysninger

import java.time.LocalDate

data class SistePeriodeMedOvergangsstønad(
    val fom: LocalDate,
    val tom: LocalDate,
    val inntekt: Int?,
    val samordningsfradrag: Int?,
)
