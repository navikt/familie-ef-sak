package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OppgaveRepository : RepositoryInterface<Oppgave, Long>, InsertUpdateRepository<Oppgave> {

    fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId: UUID, oppgavetype: Oppgavetype): Oppgave?

    fun findByGsakOppgaveId(gsakOppgaveId: Long): Oppgave?
}