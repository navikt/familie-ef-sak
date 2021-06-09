package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.mapper.IverksettingDtoMapper
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.VedtakRepository
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository;
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TeknisktOpphørService(val behandlingService: BehandlingService,
                            val behandlingRepository: BehandlingRepository,
                            val fagsakService: FagsakService,
                            val tilkjentYtelseRepository: TilkjentYtelseRepository,
                            private
                            val taskRepository: TaskRepository) {

    fun håndterTeknisktOpphør(personIdent: PersonIdent) {
        val sisteBehandling = behandlingRepository.finnSisteBehandling(Stønadstype.OVERGANGSSTØNAD, setOf(personIdent.ident))
        require(sisteBehandling != null) { throw Feil("Finner ikke behandling med stønadstype overgangsstønad for personen") }
        val fagsakId = sisteBehandling.fagsakId
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val nyBehandling = opprettBehandlingTekniskOpphør(fagsakId)
        opprettTilkjentYtelse(behandlingId = nyBehandling.id,
                              personIdent = aktivIdent)

    }

    private fun opprettTilkjentYtelse(behandlingId: UUID, personIdent: String) {
        tilkjentYtelseRepository.insert(TilkjentYtelse(behandlingId = behandlingId,
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