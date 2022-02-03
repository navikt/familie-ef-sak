package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FagsakPersonService(private val fagsakPersonRepository: FagsakPersonRepository) {

    fun hentPerson(personId: UUID): FagsakPerson = fagsakPersonRepository.findByIdOrThrow(personId)

    fun finnPerson(personIdenter: Set<String>): FagsakPerson? = fagsakPersonRepository.findByIdent(personIdenter)

    fun hentIdenter(personId: UUID): Set<PersonIdent> {
        val personIdenter = fagsakPersonRepository.findPersonIdenter(personId)
        feilHvis(personIdenter.isEmpty()) { "Finner ikke personidenter til person=$personId" }
        return personIdenter
    }

    fun hentEllerOpprettPerson(personIdenter: Set<String>, gjeldendePersonIdent: String): FagsakPerson {
        feilHvisIkke(personIdenter.contains(gjeldendePersonIdent)) {
            "Liste med personidenter inneholder ikke gjelende personident"
        }
        return (fagsakPersonRepository.findByIdent(personIdenter)
                ?: fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(gjeldendePersonIdent)))))
    }

    fun oppdaterIdent(fagsakPerson: FagsakPerson, gjeldendePersonIdent: String): FagsakPerson {
        return if (fagsakPerson.hentAktivIdent() != gjeldendePersonIdent) {
            fagsakPersonRepository.update(fagsakPerson.medOppdatertGjeldendeIdent(gjeldendePersonIdent))
        } else {
            fagsakPerson
        }
    }
}