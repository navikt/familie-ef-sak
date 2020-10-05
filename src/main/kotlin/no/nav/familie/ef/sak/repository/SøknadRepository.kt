package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Søknad
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SøknadRepository : CrudRepository<Søknad, UUID> {

    fun findByBehandlingId(behandlingId: UUID): Søknad?

}
