package no.nav.familie.ef.sak.avstemming

import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AvstemmingService(private val iverksettClient: IverksettClient,
                        private val tilkjentYtelseService: TilkjentYtelseService) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun konsistensavstemOppdrag(stønadstype: Stønadstype, datoForAvstemming: LocalDate) {
        val tilkjenteYtelser = tilkjentYtelseService
                .finnTilkjentYtelserTilKonsistensavstemming(datoForAvstemming = datoForAvstemming, stønadstype = stønadstype)
        loggKonsistensavstemming(tilkjenteYtelser)
        iverksettClient.konsistensavstemming(KonsistensavstemmingDto(StønadType.valueOf(stønadstype.name), tilkjenteYtelser))
    }

    private fun loggKonsistensavstemming(konsistensavstemming: List<KonsistensavstemmingTilkjentYtelseDto>) {
        val beløp = konsistensavstemming.sumOf { it.andelerTilkjentYtelse.sumOf(AndelTilkjentYtelseDto::beløp) }
        logger.info("Konsistensavstemming antall=${konsistensavstemming.size} beløp=$beløp")
    }

}
