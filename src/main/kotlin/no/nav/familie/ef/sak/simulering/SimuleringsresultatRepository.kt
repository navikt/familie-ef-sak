package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SimuleringsresultatRepository : RepositoryInterface<Simuleringsresultat, UUID>, InsertUpdateRepository<Simuleringsresultat>