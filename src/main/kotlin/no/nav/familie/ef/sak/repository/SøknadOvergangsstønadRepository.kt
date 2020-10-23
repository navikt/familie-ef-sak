package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SøknadOvergangsstønadRepository : RepositoryInterface<SøknadsskjemaOvergangsstønad, UUID>,
                                            InsertUpdateRepository<SøknadsskjemaOvergangsstønad>