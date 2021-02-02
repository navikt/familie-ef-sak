package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Registergrunnlag
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RegistergrunnlagRepository : RepositoryInterface<Registergrunnlag, UUID>, InsertUpdateRepository<Registergrunnlag>