package no.nav.familie.ef.sak.avstemming

import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class AvstemmingService(
    private val iverksettClient: IverksettClient,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    private val logger = Logg.getLogger(this::class)

    fun konsistensavstemOppdrag(
        stønadstype: StønadType,
        avstemmingstidspunkt: LocalDateTime,
    ) {
        val emptyDto = KonsistensavstemmingDto(stønadstype, emptyList(), avstemmingstidspunkt)
        val datoForAvstemming = avstemmingstidspunkt.toLocalDate()
        val tilkjenteYtelser =
            tilkjentYtelseService
                .finnTilkjentYtelserTilKonsistensavstemming(datoForAvstemming = datoForAvstemming, stønadstype = stønadstype)
        val transaksjonId = UUID.randomUUID()
        val chunks = tilkjenteYtelser.chunked(1000)
        loggKonsistensavstemming(stønadstype, tilkjenteYtelser, transaksjonId, chunks.size)
        iverksettClient.sendStartmeldingKonsistensavstemming(emptyDto, transaksjonId)
        chunks.forEach {
            val request = KonsistensavstemmingDto(stønadstype, it, avstemmingstidspunkt)
            iverksettClient.sendKonsistensavstemming(request, transaksjonId)
        }
        iverksettClient.sendSluttmeldingKonsistensavstemming(emptyDto, transaksjonId)
    }

    private fun loggKonsistensavstemming(
        stønadstype: StønadType,
        konsistensavstemming: List<KonsistensavstemmingTilkjentYtelseDto>,
        transaksjon: UUID,
        chunks: Int,
    ) {
        val beløp = konsistensavstemming.sumOf { it.andelerTilkjentYtelse.sumOf(AndelTilkjentYtelseDto::beløp) }
        logger.info(
            "Konsistensavstemming stønad=$stønadstype transaksjon=$transaksjon antall=${konsistensavstemming.size} " +
                "beløp=$beløp chunks=$chunks",
        )
    }
}
