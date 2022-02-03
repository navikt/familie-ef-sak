package no.nav.familie.ef.sak.testutil

import no.nav.familie.ef.sak.fagsak.FagsakPersonRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakDao
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.fagsak.domain.tilFagsak
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

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        val person = hentEllerOpprettPerson(fagsak)
        return fagsakRepository.insert(FagsakDao(id = fagsak.id,
                                                 fagsakPersonId = person.id,
                                                 stønadstype = fagsak.stønadstype,
                                                 eksternId = fagsak.eksternId,
                                                 migrert = fagsak.migrert,
                                                 sporbar = fagsak.sporbar)).tilFagsak(person.identer)
    }

    private fun hentEllerOpprettPerson(fagsak: Fagsak): FagsakPerson {
        val person = fagsakPersonRepository.findByIdOrNull(fagsak.fagsakPersonId)
        return person ?: fagsakPersonRepository.insert(FagsakPerson(fagsak.fagsakPersonId,
                                                                    identer = fagsak.personIdenter))
    }

}