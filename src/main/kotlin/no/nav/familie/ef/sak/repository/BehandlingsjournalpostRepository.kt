package no.nav.familie.ef.sak.repository


import no.nav.familie.ef.sak.repository.domain.Behandlingsjournalpost
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingsjournalpostRepository :
        RepositoryInterface<Behandlingsjournalpost, UUID>, InsertUpdateRepository<Behandlingsjournalpost> {

    fun findAllByBehandlingId(behandlingId: UUID): List<Behandlingsjournalpost>
}
