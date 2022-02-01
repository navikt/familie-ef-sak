package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.fagsak.domain.Person
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

internal class FagsakPersonServiceTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var personRepository: PersonRepository
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate
    @Autowired private lateinit var fagsakPersonService: FagsakPersonService

    @Test
    internal fun `hentEllerOpprettPerson - skal kaste feil når man spør etter identer som matcher flere personer`() {
        fagsakPersonService.hentEllerOpprettPerson(setOf("1"), "1")
        fagsakPersonService.hentEllerOpprettPerson(setOf("2"), "2")
        assertThatThrownBy { fagsakPersonService.hentEllerOpprettPerson(setOf("1", "2"), "1") }
                .isInstanceOf(IncorrectResultSizeDataAccessException::class.java)
                .hasMessageContaining("Incorrect result size: expected 1, actual 2")
    }

    @Test
    internal fun `oppdaterIdent - skal oppdatere person med ny ident`() {
        val aktivIdent = "1"
        val annenIdent = "2"
        val personId = personRepository.insert(Person(identer = setOf(PersonIdent(aktivIdent)))).id
        jdbcTemplate.update("UPDATE person_ident SET endret_tid=(endret_tid - INTERVAL '1 DAY')")

        val person = personRepository.findByIdOrThrow(personId)
        assertThat(person.hentAktivIdent()).isEqualTo(aktivIdent)
        val oppdatertPerson = fagsakPersonService.oppdaterIdent(person, annenIdent)

        assertThat(oppdatertPerson.identer.map { it.ident }).containsExactlyInAnyOrder(aktivIdent, annenIdent)
        assertThat(oppdatertPerson.hentAktivIdent()).isEqualTo(annenIdent)
    }

    @Test
    internal fun `oppdaterIdent - tidligere ident blir aktiv på nytt`() {
        val aktivIdent = "1"
        val annenIdent = "2"
        val personId = personRepository.insert(Person(identer = setOf(PersonIdent(aktivIdent), PersonIdent(annenIdent)))).id
        jdbcTemplate.update("UPDATE person_ident SET endret_tid=(endret_tid - INTERVAL '1 DAY') WHERE ident = '2'")

        val person = personRepository.findByIdOrThrow(personId)
        assertThat(person.hentAktivIdent()).isEqualTo(aktivIdent)
        val oppdatertPerson = fagsakPersonService.oppdaterIdent(person, annenIdent)

        assertThat(oppdatertPerson.identer.map { it.ident }).containsExactlyInAnyOrder(aktivIdent, annenIdent)
        assertThat(oppdatertPerson.hentAktivIdent()).isEqualTo(annenIdent)
    }

    @Test
    internal fun `hentPerson - skal hente person som finnes`() {
        val person = fagsakPersonService.hentEllerOpprettPerson(setOf("1"), "1")
        assertThat(fagsakPersonService.hentPerson(person.id)).isEqualTo(person)
    }

    @Test
    internal fun `hentPerson - skal kaste feil når person ikke finnes`() {
        assertThatThrownBy { fagsakPersonService.hentPerson(UUID.randomUUID()) }
                .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    internal fun `hentIdenter - skal kaste feil når person ikke finnes`() {
        assertThatThrownBy { fagsakPersonService.hentIdenter(UUID.randomUUID()) }
                .isInstanceOf(IllegalStateException::class.java)
    }
}