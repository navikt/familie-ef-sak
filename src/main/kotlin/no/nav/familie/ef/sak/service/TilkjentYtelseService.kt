package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.mapper.tilDto
import no.nav.familie.ef.sak.mapper.tilTilkjentYtelse
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.ef.sak.økonomi.tilKlassifisering
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TilkjentYtelseService(private val oppdragClient: OppdragClient,
                            private val behandlingService: BehandlingService,
                            private val fagsakService: FagsakService,
                            private val tilkjentYtelseRepository: TilkjentYtelseRepository
) {

    fun hentStatus(tilkjentYtelseId: UUID): OppdragStatus {

        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)
        val behandling = behandlingService.hentBehandling(tilkjentYtelse.behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val oppdragId = OppdragId(fagsystem = fagsak.stønadstype.tilKlassifisering(),
                                  personIdent = tilkjentYtelse.personident,
                                  behandlingsId = behandling.eksternId.id.toString()) //TODO SKA DET VARA LONG ELER UUID HER ???????

        return oppdragClient.hentStatus(oppdragId)
    }

    fun hentTilkjentYtelseDto(tilkjentYtelseId: UUID): TilkjentYtelseDTO {
        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)
        return tilkjentYtelse.tilDto()
    }


    @Transactional
    fun opprettTilkjentYtelse(tilkjentYtelseDTO: TilkjentYtelseDTO) {
        val nyTilkjentYtelse = tilkjentYtelseDTO.tilTilkjentYtelse()

        val behandling = behandlingService.hentBehandling(nyTilkjentYtelse.behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)


        val nyTilkjentYtelseMedEksternId = TilkjentYtelseMedMetaData(nyTilkjentYtelse,
                                                                     eksternBehandlingId = behandling.eksternId.id,
                                                                     stønadstype = fagsak.stønadstype,
                                                                     eksternFagsakId = fagsak.eksternId.id)

        val forrigeTilkjentYtelse = tilkjentYtelseRepository.finnNyesteTilkjentYtelse()

        val tilkjentYtelseMedUtbetalingsoppdrag =
                UtbetalingsoppdragGenerator
                        .lagTilkjentYtelseMedUtbetalingsoppdrag(nyTilkjentYtelseMedMetaData = nyTilkjentYtelseMedEksternId,
                                                                forrigeTilkjentYtelse = forrigeTilkjentYtelse)


        tilkjentYtelseRepository.insert(tilkjentYtelseMedUtbetalingsoppdrag)


        if (forrigeTilkjentYtelse != null && tilkjentYtelseMedUtbetalingsoppdrag.utbetalingsoppdrag != null) {
            oppdaterForrigeTilkjentYtelseMedOpphørsdato(tilkjentYtelseMedUtbetalingsoppdrag.utbetalingsoppdrag,
                                                        forrigeTilkjentYtelse)
        }

    }

    private fun oppdaterForrigeTilkjentYtelseMedOpphørsdato(utbetalingsoppdrag: Utbetalingsoppdrag,
                                                            forrigeTilkjentYtelse: TilkjentYtelse) {
        utbetalingsoppdrag.utbetalingsperiode
                .mapNotNull { it.opphør?.opphørDatoFom }
                .firstOrNull()
                .let {
                    tilkjentYtelseRepository.update(forrigeTilkjentYtelse.copy(opphørFom = it))
                }
    }

    private fun hentTilkjentYtelse(tilkjentYtelseId: UUID) =
            tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)
            ?: error("Fant ikke tilkjent ytelse med id $tilkjentYtelseId")

}
