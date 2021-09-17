package no.nav.familie.ef.sak.tilkjentytelse.domain

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TilkjentYtelse(@Id
                          val id: UUID = UUID.randomUUID(),
                          val behandlingId: UUID,
                          val personident: String,
                          val vedtakstidspunkt: LocalDateTime = SporbarUtils.now(),
                          val type: TilkjentYtelseType = TilkjentYtelseType.FØRSTEGANGSBEHANDLING,
                          val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                          @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                          val sporbar: Sporbar = Sporbar())

fun TilkjentYtelse.stønadFom(): LocalDate? = this.andelerTilkjentYtelse.minByOrNull { it.stønadFom }?.stønadFom
fun TilkjentYtelse.stønadTom(): LocalDate? = this.andelerTilkjentYtelse.minByOrNull { it.stønadFom }?.stønadFom


enum class TilkjentYtelseType {
    FØRSTEGANGSBEHANDLING,
    OPPHØR,
    ENDRING
}

