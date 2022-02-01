package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.Person
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PersonRepository : RepositoryInterface<Person, UUID>, InsertUpdateRepository<Person> {

    @Query("SELECT p.* FROM person p WHERE EXISTS(SELECT 1 FROM person_ident WHERE person_id = p.id AND ident IN (:identer))")
    fun findByIdent(identer: Set<String>): Person?

    @Query("SELECT * FROM person_ident WHERE person_id = :personId")
    fun findPersonIdenter(personId: UUID): Set<PersonIdent>

}
