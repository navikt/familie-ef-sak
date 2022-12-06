package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VedtakRepository : RepositoryInterface<Vedtak, UUID>, InsertUpdateRepository<Vedtak> {

    fun findAllByResultatType(resultatType: ResultatType): List<Vedtak>
}
