package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TestTilkjentYtelseService(private val tilkjentYtelseService: TilkjentYtelseService) {

    fun konsistensavstemOppdrag(stønadstype: Stønadstype): KonsistensavstemmingDto {
        val konsistensavstemming = tilkjentYtelseService
                .finnTilkjentYtelserTilKonsistensavstemming(stønadstype = stønadstype,
                                                            datoForAvstemming = LocalDate.now())
        return KonsistensavstemmingDto(StønadType.valueOf(stønadstype.name), konsistensavstemming)
    }

}