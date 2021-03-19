package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BlankettRepository : RepositoryInterface<Blankett, UUID>, InsertUpdateRepository<Blankett> {

}