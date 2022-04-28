package no.nav.familie.ef.sak.avstemming

import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class AvstemmingService(private val iverksettClient: IverksettClient,
                        private val tilkjentYtelseService: TilkjentYtelseService) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun konsistensavstemOppdrag(stønadstype: StønadType, datoForAvstemming: LocalDate) {
        val emptyDto = KonsistensavstemmingDto(stønadstype, emptyList())
        val tilkjenteYtelser = tilkjentYtelseService
                .finnTilkjentYtelserTilKonsistensavstemming(datoForAvstemming = datoForAvstemming, stønadstype = stønadstype)
        val transaksjon = UUID.randomUUID()
        val chunks = tilkjenteYtelser.chunked(1000)
        loggKonsistensavstemming(stønadstype, tilkjenteYtelser, transaksjon, chunks.size)
        iverksettClient.konsistensavstemming(emptyDto, true, false, transaksjon)
        chunks.forEach {
            iverksettClient.konsistensavstemming(KonsistensavstemmingDto(stønadstype, tilkjenteYtelser),
                                                 false,
                                                 false,
                                                 transaksjon)
        }
        iverksettClient.konsistensavstemming(emptyDto, false, true, transaksjon)
    }

    private fun loggKonsistensavstemming(stønadstype: StønadType,
                                         konsistensavstemming: List<KonsistensavstemmingTilkjentYtelseDto>,
                                         transaksjon: UUID,
                                         chunks: Int) {
        val beløp = konsistensavstemming.sumOf { it.andelerTilkjentYtelse.sumOf(AndelTilkjentYtelseDto::beløp) }
        logger.info("Konsistensavstemming stønad=$stønadstype antall=${konsistensavstemming.size} beløp=$beløp chunks=$chunks")
    }

}
