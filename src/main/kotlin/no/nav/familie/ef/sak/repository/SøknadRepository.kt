package no.nav.familie.ef.sak.repository


import no.nav.familie.ef.sak.repository.domain.Søknad
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SøknadRepository : RepositoryInterface<Søknad, UUID>, InsertUpdateRepository<Søknad> {

    fun findByBehandlingId(behandlingId: UUID): Søknad?
}
