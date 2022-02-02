package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.testutil.hasCauseMessageContaining
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectResultSizeDataAccessException

internal class FagsakPersonRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Test
    internal fun `skal ikke være mulig med 2 personer med samme ident`() {
        fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"))))
        fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("2"))))
        assertThatThrownBy { fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("2")))) }
                .hasRootCauseInstanceOf(PSQLException::class.java)
                .has(hasCauseMessageContaining("ERROR: duplicate key value violates unique constraint \"person_ident_pkey\"\n"))
                .has(hasCauseMessageContaining("Detail: Key (ident)=(2) already exists."))
    }

    @Test
    internal fun `skal ikke kunne oppdatere person med ident som allerede finnes på annen person`() {
        val person = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"))))
        fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("2"))))
        assertThatThrownBy { fagsakPersonRepository.update(person.medOppdatertGjeldendeIdent("2")) }
                .hasRootCauseInstanceOf(PSQLException::class.java)
                .has(hasCauseMessageContaining("ERROR: duplicate key value violates unique constraint \"person_ident_pkey\"\n"))
                .has(hasCauseMessageContaining("Detail: Key (ident)=(2) already exists."))
    }

    @Test
    internal fun `findByIdent - skal finne riktig person`() {
        val person1 = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"))))
        val person2 = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("2"))))
        assertThat(fagsakPersonRepository.findByIdent(setOf("1"))).isEqualTo(person1)
        assertThat(fagsakPersonRepository.findByIdent(setOf("2"))).isEqualTo(person2)
    }

    @Test
    internal fun `findByIdent - skal ikke være mulig å kalle på metode med identer som finnes på 2 ulike personer`() {
        fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"))))
        fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("2"))))
        assertThatThrownBy { fagsakPersonRepository.findByIdent(setOf("1", "2")) }
                .isInstanceOf(IncorrectResultSizeDataAccessException::class.java)
                .hasMessageContaining("Incorrect result size: expected 1, actual 2")
    }

    @Test
    internal fun `skal finne identer til person`() {
        val person1 = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"), PersonIdent("3"))))
        val person2 = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("2"))))
        assertThat(fagsakPersonRepository.findPersonIdenter(person1.id)).containsExactlyInAnyOrderElementsOf(person1.identer)
        assertThat(fagsakPersonRepository.findPersonIdenter(person2.id)).containsExactlyInAnyOrderElementsOf(person2.identer)
    }
}
