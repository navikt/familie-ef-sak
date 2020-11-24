package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import java.time.LocalDateTime

data class KonsistensavstemmingPayload(val stønadstype: Stønadstype, val triggerTid: LocalDateTime)