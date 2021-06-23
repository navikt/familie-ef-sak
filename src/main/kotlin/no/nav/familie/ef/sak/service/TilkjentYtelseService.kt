package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.feilHvis
import no.nav.familie.ef.sak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.util.isEqualOrAfter
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TilkjentYtelseService(private val behandlingService: BehandlingService,
                            private val featureToggleService: FeatureToggleService,
                            private val tilkjentYtelseRepository: TilkjentYtelseRepository) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentForBehandling(behandlingId: UUID): TilkjentYtelse {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
               ?: error("Fant ikke tilkjent ytelse med behandlingsid $behandlingId")
    }

    fun opprettTilkjentYtelse(nyTilkjentYtelse: TilkjentYtelse): TilkjentYtelse {
        val andelerMedGodtykkligKildeId =
                nyTilkjentYtelse.andelerTilkjentYtelse.map { it.copy(kildeBehandlingId = nyTilkjentYtelse.behandlingId) }
        return tilkjentYtelseRepository.insert(nyTilkjentYtelse.copy(andelerTilkjentYtelse = andelerMedGodtykkligKildeId))
    }

    fun finnSisteTilkjentYtelse(fagsakId: UUID): TilkjentYtelse? {
        return tilkjentYtelseRepository.finnSisteTilkjentYtelse(fagsakId)
    }

    fun finnTilkjentYtelserTilKonsistensavstemming(stønadstype: Stønadstype,
                                                   datoForAvstemming: LocalDate): List<KonsistensavstemmingTilkjentYtelseDto> {
        return behandlingService.finnSisteIverksatteBehandlinger(stønadstype)
                .chunked(1000)
                .map(List<UUID>::toSet)
                .flatMap { behandlingIder ->
                    val konsistensavstemming = finnTilkjentYtelserTilKonsistensavstemming(behandlingIder, datoForAvstemming)
                    loggKonsistensavstemming(konsistensavstemming)
                    konsistensavstemming
                }
    }

    private fun loggKonsistensavstemming(konsistensavstemming: List<KonsistensavstemmingTilkjentYtelseDto>) {
        val beløp = konsistensavstemming.sumOf { it.andelerTilkjentYtelse.sumOf(AndelTilkjentYtelseDto::beløp) }
        logger.info("Konsistensavstemming antall=${konsistensavstemming.size} beløp=$beløp")

        if (featureToggleService.isEnabled("familie.ef.sak.konsistensavstemming-logg")) {
            konsistensavstemming.forEach {
                val andeler = it.andelerTilkjentYtelse.map { aty -> "beløp=${aty.beløp} fom=${aty.fraOgMed} tom=${aty.tilOgMed}" }
                secureLogger.info("Konsistensavstemming" +
                                  " behandling=${it.behandlingId}" +
                                  " fagsak=${it.eksternFagsakId}" +
                                  " andeler=$andeler")
            }
        }
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
        feilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) { "Kan ikke reberegne tilkjent ytelse for en behandling som er låst for videre redigering" }
        tilkjentYtelseRepository.findByBehandlingId(behandlingId)?.let { tilkjentYtelseRepository.deleteById(it.id) }
    }


}
