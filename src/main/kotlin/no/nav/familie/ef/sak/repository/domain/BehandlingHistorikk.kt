package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.dto.BehandlingsHistorikkDto
import no.nav.familie.ef.sak.service.steg.StegType
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.*

data class BehandlingHistorikk(@Id
                               val id: UUID = UUID.randomUUID(),
                               val behandlingId: UUID,
                               var steg: StegType,
                               val endretAvNavn: String,
                               val endretAvMail: String,
                               val endretTid: LocalDateTime = LocalDateTime.now())

inline fun BehandlingHistorikk.tilDto(): BehandlingsHistorikkDto {
    return BehandlingsHistorikkDto(this.id,
                                   this.behandlingId,
                                   this.steg,
                                   this.endretAvNavn,
                                   this.endretAvMail,
                                   this.endretTid)
}