package no.nav.familie.ef.sak.behandling.henlegg

import java.util.UUID

data class HenleggTaskDto(
    val behandlingId: UUID,
    val saksbehandlerSignatur: String,
    val saksbehandlerIdent: String,
)
