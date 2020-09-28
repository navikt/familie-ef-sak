package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("oppgave")
data class Oppgave(@Id
                   val id: UUID = UUID.randomUUID(),
                   @Column("behandling_id")
                   val behandlingId: UUID,
                   @Column("gsak_oppgave_id")
                   val gsakOppgaveId: Long,
                   val type: Oppgavetype,
                   @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                   val sporbar: Sporbar = Sporbar(),
                   @Column("ferdigstilt")
                   var erFerdigstilt: Boolean = false)

