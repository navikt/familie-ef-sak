package no.nav.familie.ef.sak.behandling.henlegg

import java.util.UUID

data class HenleggelsesbrevDto(
    val behandlingId: UUID,
    val saksbehandlerSignatur: String,
    val saksbehandlerIdent: String,
)
