package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.mapper.tilDto
import no.nav.familie.ef.sak.mapper.tilTilkjentYtelse
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus.AKTIV
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus.AVSLUTTET
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus.IKKE_KLAR
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus.OPPRETTET
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING
import no.nav.familie.ef.sak.util.isEqualOrAfter
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.ef.sak.økonomi.tilKlassifisering
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class TilkjentYtelseService(private val oppdragClient: OppdragClient,
                            private val behandlingService: BehandlingService,
                            private val fagsakService: FagsakService,
                            private val tilkjentYtelseRepository: TilkjentYtelseRepository) {

    fun hentStatus(behandling: Behandling): OppdragStatus {
        val tilkjentYtelse = hentTilkjentYtelseFraBehandlingId(behandling.id)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val oppdragId = OppdragId(fagsystem = fagsak.stønadstype.tilKlassifisering(),
                                  personIdent = tilkjentYtelse.personident,
                                  behandlingsId = behandling.eksternId.id.toString())

        return oppdragClient.hentStatus(oppdragId)
    }

    fun hentForBehandling(behandlingId: UUID): TilkjentYtelse {
        return hentTilkjentYtelseFraBehandlingId(behandlingId)
    }

    fun hentTilkjentYtelseDto(tilkjentYtelseId: UUID): TilkjentYtelseDTO {
        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)
        return tilkjentYtelse.tilDto()
    }

    fun opprettTilkjentYtelse(tilkjentYtelseDTO: TilkjentYtelseDTO): TilkjentYtelse {
        val nyTilkjentYtelse = tilkjentYtelseDTO.tilTilkjentYtelse()
        val andelerMedGodtykkligKildeId =
                nyTilkjentYtelse.andelerTilkjentYtelse.map { it.copy(kildeBehandlingId = nyTilkjentYtelse.behandlingId) }
        return tilkjentYtelseRepository.insert(nyTilkjentYtelse.copy(andelerTilkjentYtelse = andelerMedGodtykkligKildeId))
    }

    fun finnSisteTilkjentYtelse(fagsakId: UUID): TilkjentYtelse? {
        return tilkjentYtelseRepository.finnSisteTilkjentYtelse(fagsakId)
    }

    @Transactional
    fun oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(behandling: Behandling): TilkjentYtelse {

        val nyTilkjentYtelse = hentTilkjentYtelseFraBehandlingId(behandlingId = behandling.id)

        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val nyTilkjentYtelseMedEksternId = TilkjentYtelseMedMetaData(nyTilkjentYtelse,
                                                                     eksternBehandlingId = behandling.eksternId.id,
                                                                     stønadstype = fagsak.stønadstype,
                                                                     eksternFagsakId = fagsak.eksternId.id)

        val forrigeTilkjentYtelse = tilkjentYtelseRepository.finnSisteTilkjentYtelse(fagsakId = fagsak.id)

        return UtbetalingsoppdragGenerator
                .lagTilkjentYtelseMedUtbetalingsoppdrag(nyTilkjentYtelseMedMetaData = nyTilkjentYtelseMedEksternId,
                                                        forrigeTilkjentYtelse = forrigeTilkjentYtelse)
                .let { tilkjentYtelseRepository.update(it) }
                .also {
                    oppdragClient.iverksettOppdrag(it.utbetalingsoppdrag
                                                   ?: error("utbetalingsoppdrag skal være generert i UtbetalingsoppdragGenerator"))
                }
    }

    fun finnLøpendeUtbetalninger(stønadstype: Stønadstype, datoForAvstemming: LocalDate): List<PerioderForBehandling> {
        return tilkjentYtelseRepository.finnSisteBehandlingForFagsak(stønadstype = stønadstype)
                .chunked(1000)
                .flatMap { sisteBehandlinger ->
                    val finnKildeBehandlingIdFraAndelTilkjentYtelse =
                            tilkjentYtelseRepository.finnKildeBehandlingIdFraAndelTilkjentYtelse(datoForAvstemming = datoForAvstemming,
                                                                                                 sisteBehandlinger = sisteBehandlinger)
                    return finnKildeBehandlingIdFraAndelTilkjentYtelse.groupBy({ it.first }, { it.second })
                            .map { PerioderForBehandling(it.key.toString(), it.value.toSet()) }
                }
    }

    fun finnTilkjentYtelserTilKonsistensavstemming(stønadstype: Stønadstype,
                                                   datoForAvstemming: LocalDate): List<KonsistensavstemmingTilkjentYtelseDto> {
        return behandlingService.finnSisteIverksatteBehandlinger(stønadstype)
                .chunked(1000)
                .map(List<UUID>::toSet)
                .flatMap { behandlingIder -> finnTilkjentYtelserTilKonsistensavstemming(behandlingIder, datoForAvstemming) }
    }

    private fun finnTilkjentYtelserTilKonsistensavstemming(behandlingIder: Set<UUID>,
                                                           datoForAvstemming: LocalDate): List<KonsistensavstemmingTilkjentYtelseDto> {
        val tilkjentYtelser =
                tilkjentYtelseRepository.finnTilkjentYtelserTilKonsistensavstemming(behandlingIder, datoForAvstemming)
        val eksterneIder = behandlingService.hentEksterneIder(tilkjentYtelser.map { it.behandlingId }.toSet())
                .associateBy { it.behandlingId }
        
        return tilkjentYtelser.map { tilkjentYtelse ->
            val eksternId = eksterneIder[tilkjentYtelse.behandlingId]
                            ?: error("Finner ikke eksterne id'er til behandling=${tilkjentYtelse.behandlingId}")
            val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse
                    .filter { it.stønadFom.isEqualOrAfter(datoForAvstemming) }
                    .map { it.tilIverksettDto() }
            KonsistensavstemmingTilkjentYtelseDto(behandlingId = tilkjentYtelse.behandlingId,
                                                  eksternBehandlingId = eksternId.eksternBehandlingId,
                                                  eksternFagsakId = eksternId.eksternFagsakId,
                                                  personIdent = tilkjentYtelse.personident,
                                                  andelerTilkjentYtelse = andelerTilkjentYtelse)
        }
    }

    fun slettTilkjentYtelseForBehandling(behandlingId: UUID) {
        val eksisterendeTilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        eksisterendeTilkjentYtelse?.let {
            when (it.status) {
                IKKE_KLAR, OPPRETTET -> tilkjentYtelseRepository.deleteById(it.id)
                SENDT_TIL_IVERKSETTING, AVSLUTTET, AKTIV -> error("Kan ikke reberegne tilkjent ytelse som er ${it.status}")
            }
        }
    }


    private fun hentTilkjentYtelse(tilkjentYtelseId: UUID) =
            tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)
            ?: error("Fant ikke tilkjent ytelse med id $tilkjentYtelseId")


    private fun hentTilkjentYtelseFraBehandlingId(behandlingId: UUID) =
            tilkjentYtelseRepository.findByBehandlingId(behandlingId)
            ?: error("Fant ikke tilkjent ytelse med behandlingsid $behandlingId")

}
