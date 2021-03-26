package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.FagsakDto
import no.nav.familie.ef.sak.api.dto.tilDto
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FagsakService(private val fagsakRepository: FagsakRepository,
                    private val behandlingService: BehandlingService) {

    fun hentFagsakMedBehandlinger(personIdent: String, stønadstype: Stønadstype): FagsakDto {
        val fagsak = fagsakRepository.findBySøkerIdent(personIdent, stønadstype)
                     ?: throw ApiFeil("Finner ikke fagsak for person", HttpStatus.BAD_REQUEST)
        val behandlinger = behandlingService.hentBehandlinger(fagsak.id)
        return fagsak.tilDto(behandlinger)
    }

    fun hentEllerOpprettFagsakMedBehandlinger(personIdent: String, stønadstype: Stønadstype): FagsakDto {
        val fagsak = hentEllerOpprettFagsak(personIdent, stønadstype)
        val behandlinger = behandlingService.hentBehandlinger(fagsak.id)
        return fagsak.tilDto(behandlinger)
    }

    fun hentEllerOpprettFagsak(personIdent: String,
                               stønadstype: Stønadstype): Fagsak {
        return (fagsakRepository.findBySøkerIdent(personIdent, stønadstype)
                ?: fagsakRepository.insert(Fagsak(stønadstype = stønadstype,
                                                  søkerIdenter = setOf(FagsakPerson(ident = personIdent)))))
    }

    fun hentFagsakMedBehandlinger(fagsakId: UUID): FagsakDto =
            hentFagsak(fagsakId).tilDto(behandlinger = behandlingService.hentBehandlinger(fagsakId))

    fun hentFagsak(fagsakId: UUID): Fagsak = fagsakRepository.findByIdOrThrow(fagsakId)

    fun hentFaksakForBehandling(behandlingId: UUID): Fagsak {
        return fagsakRepository.finnFagsakTilBehandling(behandlingId)
               ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")
    }

    fun hentEksternId(fagsakId: UUID): Long = fagsakRepository.findByIdOrThrow(fagsakId).eksternId.id

    fun hentAktivIdent(fagsakId: UUID): String = fagsakRepository.finnAktivIdent(fagsakId)

}