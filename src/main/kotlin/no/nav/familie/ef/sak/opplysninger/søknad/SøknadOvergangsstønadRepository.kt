package no.nav.familie.ef.sak.opplysninger.søknad

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.SøknadsskjemaOvergangsstønad
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SøknadOvergangsstønadRepository : RepositoryInterface<SøknadsskjemaOvergangsstønad, UUID>,
                                            InsertUpdateRepository<SøknadsskjemaOvergangsstønad>