package no.nav.familie.ef.sak.opplysninger.søknad

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaSkolepenger
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SøknadSkolepengerRepository : RepositoryInterface<SøknadsskjemaSkolepenger, UUID>,
                                        InsertUpdateRepository<SøknadsskjemaSkolepenger>
