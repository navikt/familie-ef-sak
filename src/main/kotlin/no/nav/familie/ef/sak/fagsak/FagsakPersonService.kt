package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.Person
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FagsakPersonService(private val personRepository: PersonRepository) {

    fun hentPerson(personId: UUID): Person = personRepository.findByIdOrThrow(personId)

    fun hentIdenter(personId: UUID): Set<PersonIdent> {
        val personIdenter = personRepository.findPersonIdenter(personId)
        feilHvis(personIdenter.isEmpty()) { "Finner ikke personidenter til person=$personId" }
        return personIdenter
    }

    fun hentEllerOpprettPerson(personIdenter: Set<String>, gjeldendePersonIdent: String): Person {
        feilHvisIkke(personIdenter.contains(gjeldendePersonIdent)) {
            "Liste med personidenter inneholder ikke gjelende personident"
        }
        val person =
                (personRepository.findByIdent(personIdenter) // TODO hvis gjeldendePersonIdent ikke er det som person mener er aktivIdent?
                 ?: personRepository.insert(Person(identer = setOf(PersonIdent(gjeldendePersonIdent)))))
        // TODO oppdater ident?
        return person
    }

    fun oppdaterIdent(person: Person, gjeldendePersonIdent: String): Person {
        return if (person.hentAktivIdent() != gjeldendePersonIdent) {
            personRepository.update(person.medOppdatertGjeldendeIdent(gjeldendePersonIdent))
        } else {
            person
        }
    }
}