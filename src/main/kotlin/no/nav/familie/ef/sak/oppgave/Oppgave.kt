package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.AktivitetspliktigAlder
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Oppgave(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val gsakOppgaveId: Long,
    val type: Oppgavetype,
    val oppgaveSubtype: OppgaveSubtype? = null,
    val barnPersonIdent: String? = null,
    val alder: AktivitetspliktigAlder? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    @Column("ferdigstilt")
    var erFerdigstilt: Boolean = false,
)
