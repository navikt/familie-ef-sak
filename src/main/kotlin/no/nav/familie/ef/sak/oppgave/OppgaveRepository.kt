package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OppgaveRepository : RepositoryInterface<Oppgave, Long>, InsertUpdateRepository<Oppgave> {

    fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId: UUID, oppgavetype: Oppgavetype): Oppgave?

    fun findByGsakOppgaveId(gsakOppgaveId: Long): Oppgave?
    fun findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId: UUID): Oppgave?
}