package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.Alder
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OppgaveRepository :
    RepositoryInterface<Oppgave, Long>,
    InsertUpdateRepository<Oppgave> {
    fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
    ): Oppgave?

    fun findByBehandlingIdAndTypeAndOppgaveSubtype(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
        oppgaveSubtype: OppgaveSubtype,
    ): Oppgave?

    fun findByBehandlingIdAndType(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
    ): List<Oppgave>?

    fun findByBehandlingIdAndTypeIn(
        behandlingId: UUID,
        oppgavetype: Set<Oppgavetype>,
    ): List<Oppgave>?

    fun findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
        behandlingId: UUID,
        oppgavetype: Set<Oppgavetype>,
    ): Oppgave?

    fun findByBehandlingIdAndBarnPersonIdentAndAlder(
        behandlingId: UUID,
        barnPersonIdent: String,
        alder: Alder?,
    ): Oppgave?

    // language=PostgreSQL
    @Query(
        """SELECT o.*         
            FROM oppgave o         
            WHERE o.alder is not null
                AND o.barn_person_ident IN (:barnPersonIdenter)
                AND o.type=:oppgavetype""",
    )
    fun findByTypeAndAlderIsNotNullAndBarnPersonIdenter(
        oppgavetype: Oppgavetype,
        barnPersonIdenter: List<String>?,
    ): List<Oppgave>

    fun findByGsakOppgaveId(gsakOppgaveId: Long): Oppgave?

    fun findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId: UUID): Oppgave?

    fun findTopByBehandlingIdAndTypeOrderBySporbarOpprettetTidDesc(
        behandlingId: UUID,
        type: Oppgavetype,
    ): Oppgave?
}
