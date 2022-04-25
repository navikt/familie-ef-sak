package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.EndringType
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TilkjentYtelseService(private val behandlingService: BehandlingService,
                            private val vedtakService: VedtakService,
                            private val tilkjentYtelseRepository: TilkjentYtelseRepository,
                            private val fagsakService: FagsakService,
                            private val vurderingService: VurderingService,
                            private val barnService: BarnService) {

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

    fun utledLøpendeUtbetalingForBarnIBarnetilsyn(behandlingId: UUID): List<UUID> {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val barnPåBehandling = barnService.finnBarnPåBehandling(behandlingId)
        val barnIdForAlleAktuelleBehandlinger = hentHistorikk(fagsak.id, behandlingId)
                .filter { it.endring?.type != EndringType.FJERNET }
                .filter { it.endring?.type != EndringType.ERSTATTET }
                .filter { it.andel.beløp > 0 && it.andel.stønadFra <= behandling.sporbar.opprettetTid.toLocalDate() && it.andel.stønadTil >= behandling.sporbar.opprettetTid.toLocalDate() }
                .map { it.andel.barn }
                .flatten()
        val behandlingsbarn = barnService.hentBehandlingBarnForBarnIder(barnIdForAlleAktuelleBehandlinger)
        return barnPåBehandling.filter { barnetViSerPå -> behandlingsbarn.any { it.personIdent == barnetViSerPå.personIdent } }
                .map { it.id }
    }

    fun finnTilkjentYtelserTilKonsistensavstemming(stønadstype: StønadType,
                                                   datoForAvstemming: LocalDate): List<KonsistensavstemmingTilkjentYtelseDto> {

        val tilkjentYtelser = tilkjentYtelseRepository.finnTilkjentYtelserTilKonsistensavstemming(stønadstype, datoForAvstemming)

        return tilkjentYtelser.chunked(PdlClient.MAKS_ANTALL_IDENTER).map { mapTilDto(it, datoForAvstemming) }.flatten()
    }

    private fun mapTilDto(tilkjenteYtelser: List<TilkjentYtelse>,
                          datoForAvstemming: LocalDate): List<KonsistensavstemmingTilkjentYtelseDto> {
        val behandlinger = behandlingService.hentBehandlinger(tilkjenteYtelser.map { it.behandlingId }.toSet())
                .associateBy { it.id }

        val fagsakerMedOppdatertPersonIdenter =
                fagsakService.fagsakerMedOppdatertePersonIdenter(behandlinger.map { it.value.fagsakId })
                        .associateBy { it.id }

        return tilkjenteYtelser.map { tilkjentYtelse ->
            val behandling = behandlinger[tilkjentYtelse.behandlingId]
                             ?: error("Finner ikke behandling for behandlingId=${tilkjentYtelse.behandlingId}")
            val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse
                    .filter { it.stønadTom.isEqualOrAfter(datoForAvstemming) }
                    .filter { it.beløp > 0 }
                    .map { it.tilIverksettDto() }

            val fagsakMedOppdatertPersonIdent = fagsakerMedOppdatertPersonIdenter[behandling.fagsakId]
                                                ?: error("Finner ikke fagsak for fagsakId=${behandling.fagsakId}")

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
        // hent vilkår for viss type hvor behandlingIder sendes inn
        val aktivitetArbeid = vurderingService.aktivitetArbeidForBehandlingIds(behandlingIder)
        return AndelHistorikkBeregner.lagHistorikk(tilkjenteYtelser,
                                                   vedtakForBehandlinger,
                                                   behandlinger,
                                                   tilOgMedBehandlingId,
                                                   aktivitetArbeid)
    }

}
