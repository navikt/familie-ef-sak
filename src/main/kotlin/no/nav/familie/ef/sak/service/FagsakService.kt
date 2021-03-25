package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.*

@Service
class FagsakService(private val fagsakRepository: FagsakRepository,
                    private val behandlingService: BehandlingService,
                    private val personService: PersonService) {

    fun hentEllerOpprettFagsak(personIdent: String, stønadstype: Stønadstype): FagsakDto {
        val fagsak = (fagsakRepository.findBySøkerIdent(personIdent, stønadstype)
                      ?: fagsakRepository.insert(Fagsak(stønadstype = stønadstype,
                                                        søkerIdenter = setOf(FagsakPerson(ident = personIdent)))))

        val behandlinger = behandlingService.hentBehandlinger(fagsak.id)

        return FagsakDto(id = fagsak.id,
                         personIdent = fagsak.hentAktivIdent(),
                         stønadstype = fagsak.stønadstype,
                         behandlinger = behandlinger)
    }

    fun hentFagsakMedBehandlinger(fagsakId: UUID): FagsakDto =
            hentFagsak(fagsakId).tilDto(behandlinger = behandlingService.hentBehandlinger(fagsakId))

    fun hentFagsak(fagsakId: UUID): Fagsak = fagsakRepository.findByIdOrThrow(fagsakId)

    fun hentEksternId(fagsakId: UUID): Long = fagsakRepository.findByIdOrThrow(fagsakId).eksternId.id

    fun hentAktivIdent(fagsakId: UUID): String = fagsakRepository.finnAktivIdent(fagsakId)

}