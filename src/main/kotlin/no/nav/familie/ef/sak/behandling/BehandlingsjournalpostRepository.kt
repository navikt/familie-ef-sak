package no.nav.familie.ef.sak.behandling


import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.behandling.domain.Behandlingsjournalpost
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingsjournalpostRepository :
        RepositoryInterface<Behandlingsjournalpost, UUID>, InsertUpdateRepository<Behandlingsjournalpost> {

    fun findAllByBehandlingId(behandlingId: UUID): List<Behandlingsjournalpost>
}
