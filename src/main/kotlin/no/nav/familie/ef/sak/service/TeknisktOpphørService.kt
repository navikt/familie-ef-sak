package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.steg.VentePåStatusFraIverksett
import no.nav.familie.ef.sak.task.PollStatusFraIverksettTask
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class TeknisktOpphørService(val behandlingService: BehandlingService,
                            val behandlingRepository: BehandlingRepository,
                            val fagsakService: FagsakService,
                            val tilkjentYtelseRepository: TilkjentYtelseRepository,
                            val iverksettClient: IverksettClient,
                            val taskRepository: TaskRepository) {

    @Transactional
    fun håndterTeknisktOpphør(personIdent: PersonIdent) {
        val sisteBehandling = behandlingRepository.finnSisteBehandling(Stønadstype.OVERGANGSSTØNAD, setOf(personIdent.ident))
        require(sisteBehandling != null) { throw ApiFeil("Finner ikke behandling med stønadstype overgangsstønad for personen") }
        val fagsakId = sisteBehandling.fagsakId
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val eksternFagsakId = fagsakService.hentEksternId(fagsakId)
        val nyBehandling = opprettBehandlingTekniskOpphør(fagsakId)
        val tilkjentYtelseTilOpphør = opprettTilkjentYtelse(behandlingId = nyBehandling.id,
                                                            personIdent = aktivIdent)

        iverksettClient.iverksettTekniskOpphør(TilkjentYtelseMedMetaData(tilkjentYtelse = tilkjentYtelseTilOpphør,
                                                                         eksternBehandlingId = nyBehandling.eksternId.id,
                                                                         stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                                                         eksternFagsakId = eksternFagsakId))

        taskRepository.save(PollStatusFraIverksettTask.opprettTask(nyBehandling.id))

    }

    private fun opprettTilkjentYtelse(behandlingId: UUID, personIdent: String): TilkjentYtelse {
        return tilkjentYtelseRepository.insert(TilkjentYtelse(behandlingId = behandlingId,
                                                              personident = personIdent,
                                                              utbetalingsoppdrag = null,
                                                              vedtaksdato = LocalDate.now(),
                                                              status = TilkjentYtelseStatus.OPPRETTET,
                                                              type = TilkjentYtelseType.OPPHØR,
                                                              andelerTilkjentYtelse = emptyList()))
    }

    private fun opprettBehandlingTekniskOpphør(fagsakId: UUID): Behandling {
        return behandlingService.opprettBehandling(behandlingType = BehandlingType.TEKNISK_OPPHØR, fagsakId = fagsakId)
    }
}