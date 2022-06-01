package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.behandling.domain.Behandling
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PatchVilkårRepository : CrudRepository<Behandling, UUID> {

    // language=PostgreSQL
    @Query(
        """SELECT b.*
                FROM fagsak f
                    JOIN behandling b ON f.id = b.fagsak_id
                    WHERE f.stonadstype = 'BARNETILSYN'
            """
    )
    fun finnBehandlingerSomHarBarnetilsyn(): List<Behandling>
}
