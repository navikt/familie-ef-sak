package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.fagsak.dto.FagsakDto
import no.nav.familie.ef.sak.fagsak.dto.tilDto
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FagsakService(private val fagsakRepository: FagsakRepository,
                    private val behandlingService: BehandlingService,
                    private val tilkjentYtelseService: TilkjentYtelseService) {

    fun hentEllerOpprettFagsakMedBehandlinger(personIdent: String, stønadstype: Stønadstype): FagsakDto {
        return fagsakTilDto(hentEllerOpprettFagsak(personIdent, stønadstype))
    }

    fun hentEllerOpprettFagsak(personIdent: String,
                               stønadstype: Stønadstype): Fagsak {
        return (fagsakRepository.findBySøkerIdent(personIdent, stønadstype)
                ?: fagsakRepository.insert(Fagsak(stønadstype = stønadstype,
                                                  søkerIdenter = setOf(FagsakPerson(ident = personIdent)))))
    }

    fun hentFagsakMedBehandlinger(fagsakId: UUID): FagsakDto {
        return fagsakTilDto(hentFagsak(fagsakId))
    }

    private fun fagsakTilDto(fagsak: Fagsak): FagsakDto {
        val behandlinger: List<Behandling> = behandlingService.hentBehandlinger(fagsak.id)
        val erLøpende = erLøpende(behandlinger)
        return fagsak.tilDto(behandlinger = behandlinger.map(Behandling::tilDto), erLøpende = erLøpende)
    }

    private fun erLøpende(behandlinger: List<Behandling>): Boolean {
        return behandlinger.filter {
            it.type != BehandlingType.BLANKETT &&
            it.resultat !== BehandlingResultat.ANNULLERT &&
            it.status == BehandlingStatus.FERDIGSTILT
        }.maxByOrNull { it.sporbar.opprettetTid }
                       ?.let { tilkjentYtelseService.harLøpendeUtbetaling(it.id) } ?: false
    }

    fun hentFagsak(fagsakId: UUID): Fagsak = fagsakRepository.findByIdOrThrow(fagsakId)

    fun hentFaksakForBehandling(behandlingId: UUID): Fagsak {
        return fagsakRepository.finnFagsakTilBehandling(behandlingId)
               ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")
    }

    fun hentEksternId(fagsakId: UUID): Long = fagsakRepository.findByIdOrThrow(fagsakId).eksternId.id

    fun hentAktivIdent(fagsakId: UUID): String = fagsakRepository.finnAktivIdent(fagsakId)

}