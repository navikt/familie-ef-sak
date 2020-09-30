package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Søknad
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SøknadRepository : CrudRepository<Søknad, UUID> {

    @Query("""SELECT
        s.*, soker.sak søker_sak, soker.fodselsnummer søker_fodselsnummer, soker.navn søker_navn
        FROM gr_soknad s
        JOIN soker soker ON soker.sak = s.id
        WHERE soker.fodselsnummer = :fødselsnummer""")
    fun findBySøkerFødselsnummer(fødselsnummer: String): List<Søknad>

    fun findByBehandlingId(behandlingId: UUID): Søknad?

    fun findTop10ByOrderBySporbar_OpprettetTidDesc(): List<Søknad>

}
