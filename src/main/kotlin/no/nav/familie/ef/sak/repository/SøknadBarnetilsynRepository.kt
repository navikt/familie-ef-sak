package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaBarnetilsyn
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SøknadBarnetilsynRepository : RepositoryInterface<SøknadsskjemaBarnetilsyn, UUID>,
                                        InsertUpdateRepository<SøknadsskjemaBarnetilsyn>
