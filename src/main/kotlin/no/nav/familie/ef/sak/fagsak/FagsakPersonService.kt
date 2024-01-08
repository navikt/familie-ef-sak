package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.minside.MinSideKafkaProducerService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakPersonService(private val fagsakPersonRepository: FagsakPersonRepository, private val kafkaMinSideKafkaProducerService: MinSideKafkaProducerService) {

    fun hentPerson(personId: UUID): FagsakPerson = fagsakPersonRepository.findByIdOrThrow(personId)

    fun hentPersoner(personId: List<UUID>): Iterable<FagsakPerson> = fagsakPersonRepository.findAllById(personId)

    fun finnPerson(personIdenter: Set<String>): FagsakPerson? = fagsakPersonRepository.findByIdent(personIdenter)

    fun hentIdenter(personId: UUID): Set<PersonIdent> {
        val personIdenter = fagsakPersonRepository.findPersonIdenter(personId)
        feilHvis(personIdenter.isEmpty()) { "Finner ikke personidenter til person=$personId" }
        return personIdenter
    }

    fun hentAktivIdent(personId: UUID): String = fagsakPersonRepository.hentAktivIdent(personId)

    @Transactional
    fun hentEllerOpprettPerson(personIdenter: Set<String>, gjeldendePersonIdent: String): FagsakPerson {
        feilHvisIkke(personIdenter.contains(gjeldendePersonIdent)) {
            "Liste med personidenter inneholder ikke gjeldende personident"
        }
        return (
            fagsakPersonRepository.findByIdent(personIdenter)
                ?: insertOgAktiverPersonIdentMinSideKafka(gjeldendePersonIdent)
            )
    }

    @Transactional
    fun oppdaterIdent(fagsakPerson: FagsakPerson, gjeldendePersonIdent: String): FagsakPerson {
        if (fagsakPerson.hentAktivIdent() != gjeldendePersonIdent) {
            kafkaMinSideKafkaProducerService.aktiver(gjeldendePersonIdent)
            return fagsakPersonRepository.update(fagsakPerson.medOppdatertGjeldendeIdent(gjeldendePersonIdent))
        } else {
            return fagsakPerson
        }
    }

    fun insertOgAktiverPersonIdentMinSideKafka(gjeldendePersonIdent: String): FagsakPerson {
        kafkaMinSideKafkaProducerService.aktiver(gjeldendePersonIdent)
        return fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(gjeldendePersonIdent))))
    }
}
