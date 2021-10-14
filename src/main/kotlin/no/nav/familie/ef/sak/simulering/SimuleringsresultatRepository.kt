package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SimuleringsresultatRepository : RepositoryInterface<Simuleringsresultat, UUID>,
                                          InsertUpdateRepository<Simuleringsresultat>