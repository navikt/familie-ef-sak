package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Sak
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SakRepository : CrudRepository<Sak, UUID>
