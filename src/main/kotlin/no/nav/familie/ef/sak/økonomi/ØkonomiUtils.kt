package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse.Companion.disjunkteAndeler
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse.Companion.snittAndeler
import java.time.LocalDate
import java.util.*

data class KjedeId(val klassifisering: String, val personIdent: String)

data class PeriodeId(val gjeldende: Long?,
                     val forrige: Long? = null)

fun AndelTilkjentYtelse.tilPeriodeId(): PeriodeId = PeriodeId(this.periodeId, this.forrigePeriodeId)

@Deprecated("Bør erstattes med å gjøre 'stønadFom' og  'stønadTom'  nullable")
val NULL_DATO: LocalDate = LocalDate.MIN

fun nullAndelTilkjentYtelse(behandlingId: UUID, personIdent: String, periodeId: PeriodeId?): AndelTilkjentYtelse =
        AndelTilkjentYtelse(beløp = 0,
                            stønadFom = NULL_DATO,
                            stønadTom = NULL_DATO,
                            personIdent = personIdent,
                            periodeId = periodeId?.gjeldende,
                            kildeBehandlingId = behandlingId,
                            forrigePeriodeId = periodeId?.forrige)

object ØkonomiUtils {

    /**
     * Lager oversikt over siste andel i hver kjede som finnes uten endring i oppdatert tilstand.
     * Vi må opphøre og eventuelt gjenoppbygge hver kjede etter denne. Må ta vare på andel og ikke kun offset da
     * filtrering av oppdaterte andeler senere skjer før offset blir satt.
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return liste med bestående andeler
     */
    fun beståendeAndelerPerKjede(forrigeKjeder: List<AndelTilkjentYtelse>,
                                 oppdaterteKjeder: List<AndelTilkjentYtelse>): List<AndelTilkjentYtelse> {
        val forrigeAndeler = forrigeKjeder.toSet()
        val oppdaterteAndeler = oppdaterteKjeder.toSet()
        val førsteEndring = forrigeAndeler.disjunkteAndeler(oppdaterteAndeler).minByOrNull { it.stønadFom }?.stønadFom
        val består = if (førsteEndring != null) forrigeAndeler.snittAndeler(oppdaterteAndeler)
                .filter { it.stønadFom.isBefore(førsteEndring) } else forrigeAndeler
        return består.sortedBy { it.periodeId }
    }

    /**
     * Tar utgangspunkt i ny tilstand og finner andeler som må bygges opp (nye, endrede og bestående etter første endring)
     *
     * @param[oppdaterteKjeder] ny tilstand
     * @param[beståendeAndelerIHverKjede] andeler man må bygge opp etter
     * @return andeler som må bygges fordelt på kjeder
     */
    fun andelerTilOpprettelse(oppdaterteKjeder: List<AndelTilkjentYtelse>,
                              beståendeAndelerIHverKjede: List<AndelTilkjentYtelse>)
            : List<AndelTilkjentYtelse> {

        val sisteBeståendeAndelIHverKjede = beståendeAndelerIHverKjede.sortedBy { it.periodeId }.lastOrNull()

        return if (sisteBeståendeAndelIHverKjede !== null) {
            oppdaterteKjeder.filter { it.stønadFom.isAfter(sisteBeståendeAndelIHverKjede.stønadTom) }
        } else {
            oppdaterteKjeder
        }
    }

    /**
     * Tar utgangspunkt i forrige tilstand og finner kjeder med andeler til opphør og tilhørende opphørsdato
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return siste andel og opphørsdato fra kjeder med opphør, returnerer null hvis det ikke finnes ett opphørsdato
     */
    fun andelTilOpphørMedDato(forrigeKjeder: List<AndelTilkjentYtelse>,
                              oppdaterteKjeder: List<AndelTilkjentYtelse>)
            : Pair<AndelTilkjentYtelse, LocalDate>? {

        val forrigeMaksDato = forrigeKjeder.map { it.stønadTom }.maxOrNull()
        val forrigeAndeler = forrigeKjeder.toSet()
        val oppdaterteAndeler = oppdaterteKjeder.toSet()
        val førsteEndring = forrigeAndeler
                .disjunkteAndeler(oppdaterteAndeler).minByOrNull { it.stønadFom }?.stønadFom

        val sisteForrigeAndel = forrigeKjeder.lastOrNull()
        return if (sisteForrigeAndel == null || førsteEndring == null || erNyPeriode(forrigeMaksDato, førsteEndring)) {
            null
        } else {
            Pair(sisteForrigeAndel, førsteEndring)
        }
    }

    /**
     * Sjekker om den nye endringen er etter maks datot for tidligere perioder
     */
    private fun erNyPeriode(forrigeMaksDato: LocalDate?, førsteEndring: LocalDate) =
            forrigeMaksDato != null && førsteEndring.isAfter(forrigeMaksDato)

}

