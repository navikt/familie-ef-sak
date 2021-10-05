package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TilbakekrevingRepository : RepositoryInterface<Tilbakekreving, UUID>, InsertUpdateRepository<Tilbakekreving>