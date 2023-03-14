package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import nonapi.io.github.classgraph.json.Id
import java.util.UUID

data class Fremleggsoppgave(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val opprettFremleggsoppgave: Boolean
)