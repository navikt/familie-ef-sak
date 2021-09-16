package no.nav.familie.ef.sak.avstemming

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.fagsak.Stønadstype
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AvstemmingService(private val iverksettClient: IverksettClient,
                        private val tilkjentYtelseService: TilkjentYtelseService,
                        private val featureToggleService: FeatureToggleService) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun konsistensavstemOppdrag(stønadstype: Stønadstype, datoForAvstemming: LocalDate) {
        val tilkjenteYtelser = tilkjentYtelseService
                .finnTilkjentYtelserTilKonsistensavstemming(datoForAvstemming = datoForAvstemming, stønadstype = stønadstype)
        loggKonsistensavstemming(tilkjenteYtelser)
        iverksettClient.konsistensavstemming(KonsistensavstemmingDto(StønadType.valueOf(stønadstype.name), tilkjenteYtelser))
    }

    private fun loggKonsistensavstemming(konsistensavstemming: List<KonsistensavstemmingTilkjentYtelseDto>) {
        val beløp = konsistensavstemming.sumOf { it.andelerTilkjentYtelse.sumOf(AndelTilkjentYtelseDto::beløp) }
        logger.info("Konsistensavstemming antall=${konsistensavstemming.size} beløp=$beløp")

        if (featureToggleService.isEnabled("familie.ef.sak.konsistensavstemming-logg")) {
            if (konsistensavstemming.size > 100) {
                logger.error("På tide å fjerne denne eller vurdere å lagre konsistensavstemming i databasen")
                return
            }
            konsistensavstemming.forEach {
                val andeler = it.andelerTilkjentYtelse.map { aty -> "beløp=${aty.beløp} fom=${aty.fraOgMed} tom=${aty.tilOgMed}" }
                secureLogger.info("Konsistensavstemming" +
                                  " behandling=${it.behandlingId}" +
                                  " fagsak=${it.eksternFagsakId}" +
                                  " andeler=$andeler")
            }
        }
    }

}
