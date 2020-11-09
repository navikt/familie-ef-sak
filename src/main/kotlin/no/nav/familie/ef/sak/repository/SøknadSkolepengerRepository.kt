package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaSkolepenger
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SøknadSkolepengerRepository : RepositoryInterface<SøknadsskjemaSkolepenger, UUID>,
                                        InsertUpdateRepository<SøknadsskjemaSkolepenger>
