package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class TilkjentYtelse(@Id
                          val id: UUID = UUID.randomUUID(),
                          val behandlingId: UUID,
                          val personident: String,
                          @Column("stonad_fom")
                          val stønadFom: LocalDate? = null, //min andeltilkjentYtelseDt
                          @Column("stonad_tom")
                          val stønadTom: LocalDate? = null,
                          @Column("opphor_fom")
                          val opphørFom: LocalDate? = null,
                          val utbetalingsoppdrag: Utbetalingsoppdrag? = null,
                          val vedtaksdato: LocalDate? = null,
                          val status: TilkjentYtelseStatus = TilkjentYtelseStatus.IKKE_KLAR,
                          val type: TilkjentYtelseType = TilkjentYtelseType.FØRSTEGANGSBEHANDLING,
                          val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                          @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                          val sporbar: Sporbar = Sporbar()) {

}

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

