package no.nav.familie.ef.sak.api.avstemming

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import java.time.LocalDate
import java.time.LocalDateTime

data class AvstemmingDto(val avstemmingType: AvstemmingType,
                         val stønadstype: Stønadstype,
                         val fraDato: LocalDate?,
                         val triggerTid: LocalDateTime)

enum class AvstemmingType {
    GRENSESNITTAVSTEMMING,
    KONSISTENSAVSTEMMING,
}