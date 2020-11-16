package no.nav.familie.ef.sak.api.avstemming

import java.time.LocalDate
import java.time.LocalDateTime

data class GrensesnittavstemmingDto(val fraDato: LocalDate, val triggerTid: LocalDateTime)