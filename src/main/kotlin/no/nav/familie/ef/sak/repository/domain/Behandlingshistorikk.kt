package no.nav.familie.ef.sak.repository.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.api.dto.BehandlingshistorikkDto
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

data class Behandlingshistorikk(@Id
                                val id: UUID = UUID.randomUUID(),
                                val behandlingId: UUID,
                                val steg: StegType,
                                val utfall: StegUtfall? = null,
                                val metadata: JsonWrapper? = null,
                                val opprettetAvNavn: String = SikkerhetContext.hentSaksbehandlerNavn(),
                                val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),
                                val endretTid: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))

inline fun Behandlingshistorikk.tilDto(): BehandlingshistorikkDto {
    return BehandlingshistorikkDto(behandlingId = this.behandlingId,
                                   steg = this.steg,
                                   endretAvNavn = this.opprettetAvNavn,
                                   endretAvMail = this.opprettetAv,
                                   endretTid = this.endretTid,
                                   utfall = this.utfall,
                                   metadata = this.metadata.tilJson())
}

fun JsonWrapper?.tilJson(): Map<Any, Any>? {
    return this?.json?.let { objectMapper.readValue<Map<Any, Any>>(it) }
}

enum class StegUtfall {
    BESLUTTE_VEDTAK_GODKJENT,
    BESLUTTE_VEDTAK_UNDERKJENT,
}