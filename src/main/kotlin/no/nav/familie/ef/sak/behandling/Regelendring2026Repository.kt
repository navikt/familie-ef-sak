package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Regelendring2026
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Regelendring2026Repository :
    RepositoryInterface<Regelendring2026, UUID>,
    InsertUpdateRepository<Regelendring2026> {
    fun findByBehandlingId(behandlingId: UUID): Regelendring2026?
}
