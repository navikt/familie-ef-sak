package no.nav.familie.ef.sak.økonomi.dto

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate

data class TilkjentYtelse(
        @Id
        val id: Long = 0,
        @Column("personIdent")
        val personIdentifikator: String,
        val saksnummer: String,
        @Column("stonad_fom")
        var stønadFom: LocalDate? = null,
        @Column("stonad_tom")
        var stønadTom: LocalDate? = null,
        @Column("opphor_fom")
        var opphørFom: LocalDate? = null,
        var utbetalingsoppdrag: String? = null,
        @Column("forrige_tilkjentytelse_id_fkey")
        val forrigeTilkjentYtelseId: Long? = null,
        val vedtaksdato: LocalDate? = null,
        val status: TilkjentYtelseStatus = TilkjentYtelseStatus.IKKE_KLAR
)

enum class TilkjentYtelseStatus() {
    IKKE_KLAR,
    OPPRETTET,
    SENDT_TIL_IVERKSETTING,
    AKTIV,
    AVSLUTTET
}


