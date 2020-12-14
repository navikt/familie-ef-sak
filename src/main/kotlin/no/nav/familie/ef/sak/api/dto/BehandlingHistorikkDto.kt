package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.service.steg.StegType
import java.time.LocalDateTime
import java.util.*

data class BehandlingsHistorikkDto(val behandlingId: UUID,
                                   var steg: StegType,
                                   val endretAvNavn: String,
                                   val endretAvMail: String,
                                   val endretTid: LocalDateTime = LocalDateTime.now())