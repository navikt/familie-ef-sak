package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.søknad.ISøknadsskjema
import no.nav.familie.ef.sak.repository.domain.søknad.Søknadsskjema
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SøknadsskjemaRepository : RepositoryInterface<Søknadsskjema, UUID>, InsertUpdateRepository<ISøknadsskjema>
