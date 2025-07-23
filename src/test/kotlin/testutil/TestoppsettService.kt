package no.nav.familie.ef.sak.testutil

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.INNVILGET
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.familie.ef.sak.fagsak.FagsakPersonRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakDomain
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.fagsak.domain.tilFagsakMedPerson
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakPerson
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Profile("integrasjonstest || local")
@Service
class TestoppsettService(
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    fun opprettPerson(ident: String) = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(ident))))

    fun opprettPerson(person: FagsakPerson) = fagsakPersonRepository.insert(person)

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        val person = hentEllerOpprettPerson(fagsak)
        return fagsakRepository
            .insert(
                FagsakDomain(
                    id = fagsak.id,
                    fagsakPersonId = person.id,
                    stønadstype = fagsak.stønadstype,
                    eksternId = fagsak.eksternId,
                    migrert = fagsak.migrert,
                    sporbar = fagsak.sporbar,
                ),
            ).tilFagsakMedPerson(person.identer)
    }

    fun lagreFagsakOgBehandlingForPersonIdent(
        personIdent: String,
        vedtakstidspunkt: LocalDateTime? = null,
    ): Behandling {
        val personMedGOmregningBehandling = fagsakPerson(identer = setOf(PersonIdent(personIdent)))
        val fagsak = lagreFagsak(fagsak(person = personMedGOmregningBehandling))
        val behandling =
            behandling(
                fagsak,
                resultat = INNVILGET,
                vedtakstidspunkt = vedtakstidspunkt,
                status = FERDIGSTILT,
            )
        return behandlingRepository.insert(behandling)
    }

    private fun hentEllerOpprettPerson(fagsak: Fagsak): FagsakPerson =
        fagsakPersonRepository.findByIdOrNull(fagsak.fagsakPersonId)
            ?: hentPersonFraIdenter(fagsak)
            ?: opprettPerson(fagsak)

    private fun hentPersonFraIdenter(fagsak: Fagsak): FagsakPerson? =
        fagsak.personIdenter
            .map { it.ident }
            .takeIf { it.isNotEmpty() }
            ?.let { fagsakPersonRepository.findByIdent(it) }

    private fun opprettPerson(fagsak: Fagsak) =
        fagsakPersonRepository.insert(
            FagsakPerson(
                fagsak.fagsakPersonId,
                identer = fagsak.personIdenter,
            ),
        )
}
