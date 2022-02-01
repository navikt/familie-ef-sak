package no.nav.familie.ef.sak.testutil

import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.PersonRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakDao
import no.nav.familie.ef.sak.fagsak.domain.Person
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.fagsak.domain.tilFagsak
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Profile("integrasjonstest")
@Service
class TestoppsettService(
        private val personRepository: PersonRepository,
        private val fagsakRepository: FagsakRepository,
) {

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        val person = hentEllerOpprettPerson(fagsak)
        return fagsakRepository.insert(FagsakDao(id = fagsak.id,
                                                 personId = person.id,
                                                 stønadstype = fagsak.stønadstype,
                                                 eksternId = fagsak.eksternId,
                                                 migrert = fagsak.migrert,
                                                 sporbar = fagsak.sporbar,
                                                 søkerIdenter = fagsak.søkerIdenter)).tilFagsak(person.identer)
    }

    private fun hentEllerOpprettPerson(fagsak: Fagsak): Person {
        return personRepository.findByIdOrNull(fagsak.personId)
               ?: personRepository.insert(Person(fagsak.personId,
                                                 identer = fagsak.søkerIdenter.map { PersonIdent(it.ident) }.toSet()))
    }

}