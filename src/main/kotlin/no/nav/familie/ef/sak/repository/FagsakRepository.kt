package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Fagsak
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FagsakRepository : RepositoryInterface<Fagsak, UUID>, InsertUpdateRepository<Fagsak>
