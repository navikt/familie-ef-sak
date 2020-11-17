package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.økonomi.KjedeId
import no.nav.familie.ef.sak.økonomi.PeriodeId
import no.nav.familie.ef.sak.økonomi.tilNullAndelTilkjentYtelse
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate

data class AndelTilkjentYtelse(@Column("belop")
                               val beløp: Int,
                               @Column("stonad_fom")
                               val stønadFom: LocalDate, /// TODO  Gjør nullable
                               @Column("stonad_tom")
                               val stønadTom: LocalDate, /// TODO  Gjør nullable
                               val personIdent: String,
                               val periodeId: Long? = null,
                               val forrigePeriodeId: Long? = null,
                               ) {

    private fun erTilsvarendeForUtbetaling(other: AndelTilkjentYtelse): Boolean {
        return (this.personIdent == other.personIdent
                && this.stønadFom == other.stønadFom
                && this.stønadTom == other.stønadTom
                && this.beløp == other.beløp
           )
    }

    fun erNull() = this.beløp == 0

    companion object {

        /**
         * Merk at det søkes snitt på visse attributter (erTilsvarendeForUtbetaling)
         * og man kun returnerer objekter fra receiver (ikke other)
         */
        fun Set<AndelTilkjentYtelse>.snittAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
            val andelerKunIDenne = this.subtractAndeler(other)
            return this.subtractAndeler(andelerKunIDenne)
        }

        fun Set<AndelTilkjentYtelse>.disjunkteAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
            val andelerKunIDenne = this.subtractAndeler(other)
            val andelerKunIAnnen = other.subtractAndeler(this)
            return andelerKunIDenne.union(andelerKunIAnnen)
        }

        fun nullAndel(kjedeId: KjedeId, periodeId: PeriodeId) =
                kjedeId.tilNullAndelTilkjentYtelse(periodeId)

        private fun Set<AndelTilkjentYtelse>.subtractAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
            return this.filter { a ->
                other.none { b -> a.erTilsvarendeForUtbetaling(b) }
            }.toSet()
        }
    }

}