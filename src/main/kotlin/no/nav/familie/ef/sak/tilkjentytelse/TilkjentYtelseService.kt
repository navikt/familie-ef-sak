package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TilkjentYtelseService(private val behandlingService: BehandlingService,
                            private val vedtakService: VedtakService,
                            private val tilkjentYtelseRepository: TilkjentYtelseRepository,
                            private val fagsakService: FagsakService) {

    fun hentForBehandling(behandlingId: UUID): TilkjentYtelse {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
               ?: error("Fant ikke tilkjent ytelse med behandlingsid $behandlingId")
    }

    fun opprettTilkjentYtelse(nyTilkjentYtelse: TilkjentYtelse): TilkjentYtelse {
        return tilkjentYtelseRepository.insert(nyTilkjentYtelse)
    }

    fun harLøpendeUtbetaling(behandlingId: UUID): Boolean {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
                       ?.let { it.andelerTilkjentYtelse.any { andel -> andel.stønadTom.isAfter(LocalDate.now()) } } ?: false
    }

    fun finnTilkjentYtelserTilKonsistensavstemming(stønadstype: StønadType,
                                                   datoForAvstemming: LocalDate): List<KonsistensavstemmingTilkjentYtelseDto> {

        val tilkjentYtelser = tilkjentYtelseRepository.finnTilkjentYtelserTilKonsistensavstemming(stønadstype, datoForAvstemming)

        val behandlinger = behandlingService.hentBehandlinger(tilkjentYtelser.map { it.behandlingId }.toSet())
                .associateBy { it.id }

        return tilkjentYtelser.map { tilkjentYtelse ->
            val behandling = behandlinger[tilkjentYtelse.behandlingId]
                             ?: error("Finner ikke eksterne id'er til behandling=${tilkjentYtelse.behandlingId}")
            val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse
                    .filter { it.stønadTom.isEqualOrAfter(datoForAvstemming) }
                    .filter { it.beløp > 0 }
                    .map { it.tilIverksettDto() }

            val fagsakMedOppdatertPersonIdent = fagsakService.fagsakMedOppdatertPersonIdent(behandling.fagsakId)

            KonsistensavstemmingTilkjentYtelseDto(behandlingId = tilkjentYtelse.behandlingId,
                                                  eksternBehandlingId = behandling.eksternId.id,
                                                  eksternFagsakId = fagsakMedOppdatertPersonIdent.eksternId.id,
                                                  personIdent = fagsakMedOppdatertPersonIdent.hentAktivIdent(),
                                                  andelerTilkjentYtelse = andelerTilkjentYtelse)
        }
    }

    fun slettTilkjentYtelseForBehandling(behandlingId: UUID) {
        brukerfeilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke reberegne tilkjent ytelse for en behandling som er låst for videre redigering"
        }
        tilkjentYtelseRepository.findByBehandlingId(behandlingId)?.let { tilkjentYtelseRepository.deleteById(it.id) }
    }

    fun hentHistorikk(fagsakId: UUID, tilOgMedBehandlingId: UUID?): List<AndelHistorikkDto> {
        val tilkjenteYtelser = tilkjentYtelseRepository.finnAlleIverksatteForFagsak(fagsakId)
        if (tilkjenteYtelser.isEmpty()) {
            return emptyList()
        }

        val behandlingIder = tilkjenteYtelser.map { it.behandlingId }.toSet()
        val vedtakForBehandlinger = vedtakService.hentVedtakForBehandlinger(behandlingIder)
        val behandlinger = behandlingService.hentBehandlinger(behandlingIder)
        return AndelHistorikkBeregner.lagHistorikk(tilkjenteYtelser, vedtakForBehandlinger, behandlinger, tilOgMedBehandlingId)
    }

}
