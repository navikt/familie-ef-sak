package no.nav.familie.ef.sak.testutil

import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.PersonRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakDao
import no.nav.familie.ef.sak.fagsak.domain.Person
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.fagsak.domain.tilFagsak
import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Profile("integrasjonstest")
@Service
class TestoppsettService(
        private val personRepository: PersonRepository,
        private val fagsakRepository: FagsakRepository,
) {

    fun opprettPerson(ident: String) = personRepository.insert(Person(identer = setOf(PersonIdent(ident))))

    fun opprettPerson(person: Person) = personRepository.insert(person)

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        val person = hentEllerOpprettPerson(fagsak)
        sjekkPersonOgFagsakInneholderSammeIdenter(person, fagsak)
        return fagsakRepository.insert(FagsakDao(id = fagsak.id,
                                                 personId = person.id,
                                                 stønadstype = fagsak.stønadstype,
                                                 eksternId = fagsak.eksternId,
                                                 migrert = fagsak.migrert,
                                                 sporbar = fagsak.sporbar,
                                                 søkerIdenter = fagsak.søkerIdenter)).tilFagsak(person.identer)
    }

    private fun hentEllerOpprettPerson(fagsak: Fagsak): Person {
        val person = personRepository.findByIdOrNull(fagsak.personId)
        return person ?: personRepository.insert(Person(fagsak.personId,
                                                        identer = fagsak.personIdenter))
    }

    /**
     * Skal slettes når identer fjernes fra fagsak
     * Sjekker att person og fagsak opprettes med samme identer, for å unngå krøll tvers de når begge finnes
     */
    private fun sjekkPersonOgFagsakInneholderSammeIdenter(person: Person,
                                                          fagsak: Fagsak) {
        assertThat(person.identer.map { it.ident }).containsExactlyInAnyOrderElementsOf(fagsak.søkerIdenter.map { it.ident })
        assertThat(person.identer.map { it.ident }).containsExactlyInAnyOrderElementsOf(fagsak.personIdenter.map { it.ident })
        if (person.identer.isNotEmpty() || fagsak.søkerIdenter.isNotEmpty()) {
            assertThat(person.hentAktivIdent()).isEqualTo(fagsak.hentAktivIdent())
        }
    }

}