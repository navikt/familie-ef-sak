package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Vedlegg
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VedleggRepository : RepositoryInterface<Vedlegg, UUID>
