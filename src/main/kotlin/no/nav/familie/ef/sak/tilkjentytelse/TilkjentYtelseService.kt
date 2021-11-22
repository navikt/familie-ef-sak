package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TilkjentYtelseService(private val behandlingService: BehandlingService,
                            private val vedtakService: VedtakService,
                            private val tilkjentYtelseRepository: TilkjentYtelseRepository) {

    fun hentForBehandling(behandlingId: UUID): TilkjentYtelse {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
               ?: error("Fant ikke tilkjent ytelse med behandlingsid $behandlingId")
    }

    fun opprettTilkjentYtelse(nyTilkjentYtelse: TilkjentYtelse): TilkjentYtelse {
        val andelerMedGodtykkligKildeId =
                nyTilkjentYtelse.andelerTilkjentYtelse.map { it.copy(kildeBehandlingId = nyTilkjentYtelse.behandlingId) }
        return tilkjentYtelseRepository.insert(nyTilkjentYtelse.copy(andelerTilkjentYtelse = andelerMedGodtykkligKildeId))
    }

    fun harLøpendeUtbetaling(behandlingId: UUID): Boolean {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
                       ?.let { it.andelerTilkjentYtelse.any { andel -> andel.stønadTom.isAfter(LocalDate.now()) } } ?: false
    }

    fun finnTilkjentYtelserTilKonsistensavstemming(stønadstype: Stønadstype,
                                                   datoForAvstemming: LocalDate): List<KonsistensavstemmingTilkjentYtelseDto> {
        return behandlingService.finnSisteIverksatteBehandlinger(stønadstype)
                .chunked(1000)
                .map(List<UUID>::toSet)
                .flatMap { behandlingIder -> finnTilkjentYtelserTilKonsistensavstemming(behandlingIder, datoForAvstemming) }
    }

    private fun finnTilkjentYtelserTilKonsistensavstemming(behandlingIder: Set<UUID>,
                                                           datoForAvstemming: LocalDate)
            : List<KonsistensavstemmingTilkjentYtelseDto> {
        val tilkjentYtelser =
                tilkjentYtelseRepository.finnTilkjentYtelserTilKonsistensavstemming(behandlingIder, datoForAvstemming)
        val eksterneIder = behandlingService.hentEksterneIder(tilkjentYtelser.map { it.behandlingId }.toSet())
                .associateBy { it.behandlingId }

        return tilkjentYtelser.map { tilkjentYtelse ->
            val eksternId = eksterneIder[tilkjentYtelse.behandlingId]
                            ?: error("Finner ikke eksterne id'er til behandling=${tilkjentYtelse.behandlingId}")
            val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse
                    .filter { it.stønadTom.isEqualOrAfter(datoForAvstemming) }
                    .filter { it.beløp > 0 }
                    .map { it.tilIverksettDto() }
            KonsistensavstemmingTilkjentYtelseDto(behandlingId = tilkjentYtelse.behandlingId,
                                                  eksternBehandlingId = eksternId.eksternBehandlingId,
                                                  eksternFagsakId = eksternId.eksternFagsakId,
                                                  personIdent = tilkjentYtelse.personident,
                                                  andelerTilkjentYtelse = andelerTilkjentYtelse)
        }
    }

    fun slettTilkjentYtelseForBehandling(behandlingId: UUID) {
        feilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke reberegne tilkjent ytelse for en behandling som er låst for videre redigering"
        }
        tilkjentYtelseRepository.findByBehandlingId(behandlingId)?.let { tilkjentYtelseRepository.deleteById(it.id) }
    }

    fun hentHistorikk(fagsakId: UUID): List<AndelHistorikkDto> {
        val tilkjenteYtelser = tilkjentYtelseRepository.finnAlleIverksatteForFagsak(fagsakId)
        val behandlingIder = tilkjenteYtelser.map { it.behandlingId }.toSet()
        val vedtakForBehandlinger = vedtakService.hentVedtakForBehandlinger(behandlingIder)
        val behandlinger = behandlingService.hentBehandlinger(behandlingIder)
        return AndelHistorikkBeregner.lagHistorikk(tilkjenteYtelser, vedtakForBehandlinger, behandlinger)
    }

}
