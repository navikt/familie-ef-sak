package no.nav.familie.ef.sak.oppfølgingsoppgave.domain

import no.nav.familie.ef.sak.brev.Brevmal
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("automatisk_brev")
class AutomatiskBrev(
    @Id
    val behandlingId: UUID,
    val brevSomSkalSendes: List<Brevmal>,
)
