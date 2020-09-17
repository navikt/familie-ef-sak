package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OppgaveRepository : CrudRepository<Oppgave, Long> {

    //@Query(value = "SELECT o FROM Oppgave o WHERE o.behandling = :behandling AND o.type = :oppgavetype")
    //fun findByOppgavetypeAndBehandling(behandling: Behandling, oppgavetype: Oppgavetype): Oppgave?
    //fun findByBehandlingIdAndOppgavetype(behandlingId: UUID, oppgavetype: Oppgavetype): Oppgave?

    //@Query(value = "SELECT o FROM Oppgave o WHERE o.erFerdigstilt = false AND o.behandling = :behandling AND o.type = :oppgavetype")
    //fun findBy_BehandlingId_AndOppgavetype_AndErFerdigstiltIsFalse(behandlingId: UUID, oppgavetype: Oppgavetype): Oppgave?
}