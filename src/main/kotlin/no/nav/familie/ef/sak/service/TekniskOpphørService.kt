package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.task.PollStatusTekniskOpphør
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.TekniskOpphørDto
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class TekniskOpphørService(val behandlingService: BehandlingService,
                           val behandlingRepository: BehandlingRepository,
                           val fagsakService: FagsakService,
                           val tilkjentYtelseRepository: TilkjentYtelseRepository,
                           val iverksettClient: IverksettClient,
                           val taskRepository: TaskRepository) {

    @Transactional
    fun håndterTeknisktOpphør(personIdent: PersonIdent) {
        val sisteFerdigstilteBehandling =
                behandlingRepository.finnSisteIverksatteBehandling(stønadstype = Stønadstype.OVERGANGSSTØNAD, personidenter = setOf(personIdent.ident))
        require(sisteFerdigstilteBehandling != null) { throw Feil("Finner ikke behandling med stønadstype overgangsstønad") }
        val fagsakId = sisteFerdigstilteBehandling.fagsakId
        val sisteBehandling = behandlingService.hentBehandlinger(fagsakId)
                .maxByOrNull { it.opprettet }!!

        require(sisteBehandling.id == sisteFerdigstilteBehandling.id) { throw Feil("Kan ikke utføre teknisk opphør på en aktiv behandling") }

        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val eksternFagsakId = fagsakService.hentEksternId(fagsakId)
        val nyBehandling = opprettBehandlingTekniskOpphør(fagsakId)
        val tilkjentYtelseTilOpphør = opprettTilkjentYtelse(behandlingId = nyBehandling.id,
                                                            personIdent = aktivIdent)

        taskRepository.save(PollStatusTekniskOpphør.opprettTask(nyBehandling.id))

        iverksettClient.iverksettTekniskOpphør(TekniskOpphørDto(forrigeBehandlingId = sisteFerdigstilteBehandling.id,
                                                                saksbehandlerId = tilkjentYtelseTilOpphør.sporbar.opprettetAv,
                                                                eksternBehandlingId = nyBehandling.eksternId.id,
                                                                stønadstype = StønadType.OVERGANGSSTØNAD,
                                                                eksternFagsakId = eksternFagsakId,
                                                                personIdent = aktivIdent,
                                                                behandlingId = nyBehandling.id,
                                                                vedtaksdato = LocalDate.now()))

    }

    private fun opprettTilkjentYtelse(behandlingId: UUID, personIdent: String): TilkjentYtelse {
        return tilkjentYtelseRepository.insert(TilkjentYtelse(behandlingId = behandlingId,
                                                              personident = personIdent,
                                                              vedtaksdato = LocalDate.now(),
                                                              type = TilkjentYtelseType.OPPHØR,
                                                              andelerTilkjentYtelse = emptyList()))
    }

    private fun opprettBehandlingTekniskOpphør(fagsakId: UUID): Behandling {
        return behandlingService.opprettBehandling(behandlingType = BehandlingType.TEKNISK_OPPHØR,
                                                   fagsakId = fagsakId,
                                                   status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                                   stegType = StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
    }
}