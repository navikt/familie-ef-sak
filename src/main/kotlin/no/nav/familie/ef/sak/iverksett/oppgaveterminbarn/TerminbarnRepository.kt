package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository interface TerminbarnRepository : RepositoryInterface<TerminbarnOppgave, UUID>,
                                             InsertUpdateRepository<TerminbarnOppgave> {

    @Query
    public fun existsByFagsakIdAndTermindato(fagsakId: UUID, termindato: LocalDate): Boolean
}
