package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.fagsak.FagsakDto
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.*

@Service
class FagsakService(private val fagsakRepository: FagsakRepository, private val behandlingService: BehandlingService) {

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

    fun hentFagsak(fagsakId: UUID): Fagsak = fagsakRepository.findByIdOrThrow(fagsakId)

    fun hentFagsak(eksternFagsakId: Long): Fagsak = fagsakRepository.finnMedEksternId(eksternFagsakId)
                                                    ?: throw error("Fagsak med id $eksternFagsakId finnes ikke")

    fun hentEksternId(fagsakId: UUID): Long = fagsakRepository.findByIdOrThrow(fagsakId).eksternId.id

}