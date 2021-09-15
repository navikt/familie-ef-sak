package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.task.PollStatusTekniskOpphør
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.TekniskOpphørDto
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
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


    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun håndterTeknisktOpphør(fagsakId: UUID) {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val eksternFagsakId = fagsakService.hentEksternId(fagsakId)
        val behandling = opprettBehandlingTekniskOpphør(fagsakId)
        logger.info("Utfører teknisk opphør behandling=${behandling.id}")
        val tilkjentYtelseTilOpphør = opprettTilkjentYtelse(behandlingId = behandling.id,
                                                            personIdent = aktivIdent)

        taskRepository.save(PollStatusTekniskOpphør.opprettTask(behandling.id, aktivIdent))

        iverksettClient.iverksettTekniskOpphør(TekniskOpphørDto(forrigeBehandlingId = behandling.forrigeBehandlingId!!,
                                                                saksbehandlerId = tilkjentYtelseTilOpphør.sporbar.opprettetAv,
                                                                eksternBehandlingId = behandling.eksternId.id,
                                                                stønadstype = StønadType.OVERGANGSSTØNAD,
                                                                eksternFagsakId = eksternFagsakId,
                                                                personIdent = aktivIdent,
                                                                behandlingId = behandling.id,
                                                                vedtaksdato = LocalDate.now()))
    }

    private fun opprettTilkjentYtelse(behandlingId: UUID, personIdent: String): TilkjentYtelse {
        return tilkjentYtelseRepository.insert(TilkjentYtelse(behandlingId = behandlingId,
                                                              personident = personIdent,
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