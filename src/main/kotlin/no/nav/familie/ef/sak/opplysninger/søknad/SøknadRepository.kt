package no.nav.familie.ef.sak.opplysninger.søknad


import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknad
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface SøknadRepository : RepositoryInterface<Søknad, UUID>, InsertUpdateRepository<Søknad> {

    fun findByBehandlingId(behandlingId: UUID): Søknad?

    // language=PostgreSQL
    @Query("""SELECT ss.dato_mottatt
                    FROM grunnlag_soknad soknad
                    JOIN soknadsskjema ss ON soknad.soknadsskjema_id = ss.id
                    WHERE soknad.behandling_id = :behandlingId""")
    fun finnDatoMottattForSøknad(behandlingId: UUID): LocalDateTime
}
