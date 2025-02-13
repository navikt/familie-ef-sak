package no.nav.familie.ef.sak.oppfølgingsoppgave.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("oppgaver_for_ferdigstilling")
data class OppgaverForFerdigstilling(
    @Id
    val behandlingId: UUID,
    val fremleggsoppgaveIderSomSkalFerdigstilles: List<Long>,
)
