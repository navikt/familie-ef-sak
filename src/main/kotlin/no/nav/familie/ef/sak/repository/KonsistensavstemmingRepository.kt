package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Konsistensavstemming
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KonsistensavstemmingRepository : RepositoryInterface<Konsistensavstemming, UUID>,
                                           InsertUpdateRepository<Konsistensavstemming> {

    // language=PostgreSQL
    @Query("""SELECT * FROM konsistenavstemming 
                    WHERE stonadstype = :stønadstype AND dato = current_date""")
    fun finnKonsistensavstemmingMedDatoIdag(stønadstype: Stønadstype): Konsistensavstemming?
}
