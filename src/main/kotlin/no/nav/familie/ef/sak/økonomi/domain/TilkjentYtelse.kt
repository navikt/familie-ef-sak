package no.nav.familie.ef.sak.økonomi.domain

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.*

data class TilkjentYtelse(@Id
                          val id: UUID = UUID.randomUUID(),
                          @Column("periode_id_start")
                          val periodeIdStart: Long,
                          val personident: String,
                          val saksnummer: String,
                          @Column("stonad_fom")
                          val stønadFom: LocalDate? = null,
                          @Column("stonad_tom")
                          val stønadTom: LocalDate? = null,
                          @Column("opphor_fom")
                          val opphørFom: LocalDate? = null,
                          val utbetalingsoppdrag: Utbetalingsoppdrag? = null,
                          @Column("forrige_periode_id_start")
                          val forrigePeriodeIdStart: Long? = null,
                          val vedtaksdato: LocalDate? = null,
                          val status: TilkjentYtelseStatus = TilkjentYtelseStatus.IKKE_KLAR,
                          val type: TilkjentYtelseType = TilkjentYtelseType.FØRSTEGANGSBEHANDLING,
                          val andelerTilkjentYtelse: List<AndelTilkjentYtelse>)

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

