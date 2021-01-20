package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.dto.BehandlingshistorikkDto
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

data class Behandlingshistorikk(@Id
                                val id: UUID = UUID.randomUUID(),
                                val behandlingId: UUID,
                                val steg: StegType,
                                val opprettetAvNavn: String = SikkerhetContext.hentSaksbehandlerNavn(),
                                val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),
                                val endretTid: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))

inline fun Behandlingshistorikk.tilDto(): BehandlingshistorikkDto {
    return BehandlingshistorikkDto(this.behandlingId,
                                   this.steg,
                                   this.opprettetAvNavn,
                                   this.opprettetAv,
                                   this.endretTid)
}