package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling> {

    fun findByFagsakId(fagsakId: UUID): List<Behandling>

    fun findByFagsakIdAndAktivIsTrue(fagsakId: UUID): Behandling?

    fun findByFagsakIdAndStatus(fagsakId: UUID, status: BehandlingStatus): List<Behandling>

}
