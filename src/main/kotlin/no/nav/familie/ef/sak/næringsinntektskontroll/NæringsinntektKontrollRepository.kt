package no.nav.familie.ef.sak.næringsinntektskontroll

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NæringsinntektKontrollRepository :
    RepositoryInterface<NæringsinntektKontrollDomain, UUID>,
    InsertUpdateRepository<NæringsinntektKontrollDomain>
