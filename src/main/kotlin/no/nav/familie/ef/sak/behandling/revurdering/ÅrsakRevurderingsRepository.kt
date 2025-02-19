package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ÅrsakRevurderingsRepository :
    RepositoryInterface<ÅrsakRevurdering, UUID>,
    InsertUpdateRepository<ÅrsakRevurdering>
