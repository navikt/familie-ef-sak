package no.nav.familie.ef.sak.økonomi.domain

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.*

data class TilkjentYtelse(
        @Id
        val id: Long = 0,
        @Column("ekstern_id")
        val eksternId: UUID = UUID.randomUUID(),
        @Column("personident")
        val personIdentifikator: String,
        val saksnummer: String,
        @Column("stonad_fom")
        val stønadFom: LocalDate? = null,
        @Column("stonad_tom")
        val stønadTom: LocalDate? = null,
        @Column("opphor_fom")
        val opphørFom: LocalDate? = null,
        val utbetalingsoppdrag: Utbetalingsoppdrag? = null,
        @Column("forrige_tilkjent_ytelse_id_fkey")
        val forrigeTilkjentYtelseId: Long? = null,
        val vedtaksdato: LocalDate? = null,
        val status: TilkjentYtelseStatus = TilkjentYtelseStatus.IKKE_KLAR,
        val type: TilkjentYtelseType = TilkjentYtelseType.FØRSTEGANGSBEHANDLING
)

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

