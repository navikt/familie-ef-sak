package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OppgaveRepository : CrudRepository<Oppgave, Long> {

    fun findByBehandlingIdAndType(behandlingId: UUID, oppgavetype: Oppgavetype): Oppgave?

    fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId: UUID, oppgavetype: Oppgavetype): Oppgave?
}