package no.nav.familie.ef.sak.repository

import org.springframework.data.repository.CrudRepository
import java.util.*

interface SakRepository : CrudRepository<Sak, UUID>
