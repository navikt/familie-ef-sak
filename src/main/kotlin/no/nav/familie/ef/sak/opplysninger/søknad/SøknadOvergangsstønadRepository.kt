package no.nav.familie.ef.sak.opplysninger.søknad

import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SøknadOvergangsstønadRepository : RepositoryInterface<SøknadsskjemaOvergangsstønad, UUID>,
                                            InsertUpdateRepository<SøknadsskjemaOvergangsstønad>