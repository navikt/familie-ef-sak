package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.minside.AktiverMikrofrontendNyttFødselsnummerTask
import no.nav.familie.ef.sak.minside.AktiverMikrofrontendTask
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class FagsakPersonService(
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val taskService: TaskService,
) {
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
    fun hentEllerOpprettPerson(
        personIdenter: Set<String>,
        gjeldendePersonIdent: String,
    ): FagsakPerson {
        feilHvisIkke(personIdenter.contains(gjeldendePersonIdent)) {
            "Liste med personidenter inneholder ikke gjeldende personident"
        }
        return (
            fagsakPersonRepository.findByIdent(personIdenter)
                ?: opprettFagsakPersonOgAktiverForMinSide(gjeldendePersonIdent)
        )
    }

    @Transactional
    fun oppdaterIdent(
        fagsakPerson: FagsakPerson,
        gjeldendePersonIdent: String,
    ): FagsakPerson {
        if (fagsakPerson.hentAktivIdent() != gjeldendePersonIdent) {
            val oppdatertFagsakPerson = fagsakPerson.medOppdatertGjeldendeIdent(gjeldendePersonIdent)
            taskService.save(AktiverMikrofrontendNyttFødselsnummerTask.opprettTask(oppdatertFagsakPerson))
            return fagsakPersonRepository.update(oppdatertFagsakPerson)
        } else {
            return fagsakPerson
        }
    }

    fun opprettFagsakPersonOgAktiverForMinSide(gjeldendePersonIdent: String): FagsakPerson {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(gjeldendePersonIdent))))
        taskService.save(AktiverMikrofrontendTask.opprettTask(fagsakPerson))
        return fagsakPerson
    }

    fun finnFagsakPersonForFagsakId(fagsakId: UUID): FagsakPerson = fagsakPersonRepository.finnFagsakPersonForFagsakId(fagsakId)

    fun oppdaterMedMikrofrontendAktivering(
        fagsakPersonId: UUID,
        aktivert: Boolean,
    ) {
        val fagsakPerson = hentPerson(fagsakPersonId).copy(harAktivertMikrofrontend = aktivert)
        fagsakPersonRepository.update(fagsakPerson)
    }

    /**
     * Regler  for når mikrofrontend skal deaktiveres:
     * FagsakPerson.harAktivertMikrofrontend = true
     *
     * En av følgende
     * - Ingen behandlinger og FagsakPerson opprettet for mer enn 1 mnd siden
     * - Siste utbetaling for mer enn 4 år siden, ingen nye behandlinger siste 6 mnd og ingen åpne behandlinger
     * - Kun henlagte behandlinger og siste behandling er avsluttet for mer enn 6 mnd siden
     * - Kun avslåtte og henlagte behandlinger og siste behandling er mer enn 4 år gammmel
     */
    fun finnFagsakpersonIderKlarForDeaktiveringAvMikrofrontend(): List<UUID> {
        val enMånedSiden = LocalDate.now().minusMonths(1)
        val seksMånederSiden = LocalDate.now().minusMonths(6)
        val fireÅrSiden = LocalDate.now().minusYears(4)
        val fpIderUtenBehandling = fagsakPersonRepository.finnFagsakPersonIderUtenBehandlingAktivertMikrofrontendOgEldreEnn(enMånedSiden)
        val fpIderUtenÅpenBehandlingOgSisteUtbetalingMerEnnFireÅrSiden =
            fagsakPersonRepository.finnFagsakPersonIderMedAktivertMikrofrontendOgSisteUtbetalingEldreEnnOgSistEndretEldreEnn(fireÅrSiden, seksMånederSiden)
        val fpIderForDeSomKunHarHenlagteBehandlinger =
            fagsakPersonRepository.finnFagsakPersonIderForDeSomKunHarHenlagteBehandlingerOgAktivertMikrofrontend(seksMånederSiden)
        val fpIderForDeSomKunHarAvslåtteBehandlinger =
            fagsakPersonRepository.finnFagsakPersonIderForDeSomKunHarAvslåtteBehandlingerOgAktivertMikrofrontend(fireÅrSiden)

        return fpIderUtenBehandling + fpIderUtenÅpenBehandlingOgSisteUtbetalingMerEnnFireÅrSiden + fpIderForDeSomKunHarAvslåtteBehandlinger + fpIderForDeSomKunHarHenlagteBehandlinger
    }
}
