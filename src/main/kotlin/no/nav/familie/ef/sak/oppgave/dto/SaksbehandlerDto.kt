package no.nav.familie.ef.sak.oppgave.dto

import java.util.UUID

data class SaksbehandlerDto(
    val azureId: UUID,
    val enhet: String,
    val etternavn: String,
    val fornavn: String,
    val navIdent: String,
    val rolle: SaksbehandlerRolle,
)

enum class SaksbehandlerRolle {
    IKKE_SATT,
    INNLOGGET_SAKSBEHANDLER,
    ANNEN_SAKSBEHANDLER,
}
