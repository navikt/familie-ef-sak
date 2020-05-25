package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Sak
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SakRepository : CrudRepository<Sak, UUID> {

    @Query("SELECT" +
           " s.*," +
           " soker.sak søker_sak, soker.fodselsnummer søker_fodselsnummer, soker.navn søker_navn" +
           " FROM sak s" +
           " JOIN soker soker ON soker.sak = s.id WHERE soker.fodselsnummer = :fødselsnummer")
    fun findBySøkerFødselsnummer(fødselsnummer: String): List<Sak>

}

