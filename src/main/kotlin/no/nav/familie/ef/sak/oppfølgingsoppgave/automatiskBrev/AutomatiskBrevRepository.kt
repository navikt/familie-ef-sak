package no.nav.familie.ef.sak.oppfølgingsoppgave.automatiskBrev

import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.AutomatiskBrev
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AutomatiskBrevRepository :
    RepositoryInterface<AutomatiskBrev, UUID>,
    InsertUpdateRepository<AutomatiskBrev> {
    @Modifying
    @Query(
        "DELETE from automatisk_brev where behandling_id = :behandlingId",
    )
    fun deleteByBehandlingId(behandlingId: UUID)
}
