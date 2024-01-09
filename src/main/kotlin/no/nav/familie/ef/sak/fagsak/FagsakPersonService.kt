package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.minside.MikrofrontendEnableBrukereTask
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakPersonService(private val fagsakPersonRepository: FagsakPersonRepository, private val taskService: TaskService) {

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
                ?: opprettFagsakPersonOgAktiverForMinSide(gjeldendePersonIdent)
            )
    }

    @Transactional
    fun oppdaterIdent(fagsakPerson: FagsakPerson, gjeldendePersonIdent: String): FagsakPerson {
        if (fagsakPerson.hentAktivIdent() != gjeldendePersonIdent) {
            val oppdatertFagsakPerson = fagsakPerson.medOppdatertGjeldendeIdent(gjeldendePersonIdent)
            taskService.save(MikrofrontendEnableBrukereTask.opprettTask(oppdatertFagsakPerson))
            return fagsakPersonRepository.update(oppdatertFagsakPerson)
        } else {
            return fagsakPerson
        }
    }

    fun opprettFagsakPersonOgAktiverForMinSide(gjeldendePersonIdent: String): FagsakPerson {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(gjeldendePersonIdent))))
        taskService.save(MikrofrontendEnableBrukereTask.opprettTask(fagsakPerson))
        return fagsakPerson
    }
}
