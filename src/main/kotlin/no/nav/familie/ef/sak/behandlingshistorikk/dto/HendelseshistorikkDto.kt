package no.nav.familie.ef.sak.behandlingshistorikk.dto

import java.time.LocalDateTime
import java.util.UUID

data class HendelseshistorikkDto(val behandlingId: UUID,
                                      var hendelse: Hendelse,
                                      val endretAvNavn: String,
                                      val endretTid: LocalDateTime,
                                      val metadata: Map<String, Any>? = null)

enum class Hendelse {
    OPPRETTET,
    SENDT_TIL_BESLUTTER,
    VEDTAK_GODKJENT,
    VEDTAK_UNDERKJENT,
    VEDTAK_IVERKSATT,
    VEDTAK_AVSLÅTT,
    VEDTAK_HENLAGT,
    HENLAGT,
    UKJENT
}
