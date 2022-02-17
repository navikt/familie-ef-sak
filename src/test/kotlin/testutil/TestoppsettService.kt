package no.nav.familie.ef.sak.testutil

import no.nav.familie.ef.sak.fagsak.FagsakPersonRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakMedPerson
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.fagsak.domain.tilFagsakMedPerson
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Profile("integrasjonstest")
@Service
class TestoppsettService(
        private val fagsakPersonRepository: FagsakPersonRepository,
        private val fagsakRepository: FagsakRepository,
) {

    fun opprettPerson(ident: String) = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(ident))))

    fun opprettPerson(person: FagsakPerson) = fagsakPersonRepository.insert(person)

    fun lagreFagsak(fagsakMedPerson: FagsakMedPerson): FagsakMedPerson {
        val person = hentEllerOpprettPerson(fagsakMedPerson)
        return fagsakRepository.insert(Fagsak(id = fagsakMedPerson.id,
                                              fagsakPersonId = person.id,
                                              stønadstype = fagsakMedPerson.stønadstype,
                                              eksternId = fagsakMedPerson.eksternId,
                                              migrert = fagsakMedPerson.migrert,
                                              sporbar = fagsakMedPerson.sporbar)).tilFagsakMedPerson(person.identer)
    }

    private fun hentEllerOpprettPerson(fagsakMedPerson: FagsakMedPerson): FagsakPerson {
        val person = fagsakPersonRepository.findByIdOrNull(fagsakMedPerson.fagsakPersonId)
        return person ?: fagsakPersonRepository.insert(FagsakPerson(fagsakMedPerson.fagsakPersonId,
                                                                    identer = fagsakMedPerson.personIdenter))
    }

}