package no.nav.familie.ef.sak.behandlingshistorikk.dto

import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingshistorikkDto(val behandlingId: UUID,
                                   var steg: StegType,
                                   val endretAvNavn: String,
                                   val endretAv: String,
                                   val endretTid: LocalDateTime = LocalDateTime.now(),
                                   val utfall: StegUtfall? = null,
                                   val metadata: Map<String, Any>? = null)