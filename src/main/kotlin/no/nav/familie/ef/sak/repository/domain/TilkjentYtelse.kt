package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class TilkjentYtelse(@Id
                          val id: UUID = UUID.randomUUID(),
                          val behandlingId: UUID,
                          val personident: String,
                          //TODO borde vi droppe denna kolonnen eller borde ventepåsvarfraiverksett returnere utbetalingsoppdrag?
                          val utbetalingsoppdrag: Utbetalingsoppdrag? = null,
                          val vedtaksdato: LocalDate? = null,
                          //TODO droppe ? TilkjentYtelseStatus
                          val status: TilkjentYtelseStatus = TilkjentYtelseStatus.IKKE_KLAR,
                          val type: TilkjentYtelseType = TilkjentYtelseType.FØRSTEGANGSBEHANDLING,
                          val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                          @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                          val sporbar: Sporbar = Sporbar())

fun TilkjentYtelse.stønadFom(): LocalDate? = this.andelerTilkjentYtelse.minByOrNull { it.stønadFom }?.stønadFom
fun TilkjentYtelse.stønadTom(): LocalDate? = this.andelerTilkjentYtelse.minByOrNull { it.stønadFom }?.stønadFom

enum class TilkjentYtelseStatus {
    IKKE_KLAR,
    OPPRETTET,
    SENDT_TIL_IVERKSETTING,
    AKTIV,
    AVSLUTTET
}

enum class TilkjentYtelseType {
    FØRSTEGANGSBEHANDLING,
    OPPHØR,
    ENDRING
}

