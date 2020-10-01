package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Sak
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SakRepository : RepositoryInterface<Sak, UUID>
