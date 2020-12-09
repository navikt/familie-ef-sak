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
     * @param[forrigeKjede] forrige behandlings tilstand
     * @param[oppdaterteKjede] nåværende tilstand
     * @return liste med bestående andeler
     */
    fun beståendeAndelerIKjede(forrigeKjede: List<AndelTilkjentYtelse>,
                               oppdaterteKjede: List<AndelTilkjentYtelse>): List<AndelTilkjentYtelse> {
        val forrigeAndeler = forrigeKjede.toSet()
        val oppdaterteAndeler = oppdaterteKjede.toSet()
        val førsteEndring = forrigeAndeler.disjunkteAndeler(oppdaterteAndeler).minByOrNull { it.stønadFom }?.stønadFom
        val består = if (førsteEndring != null) forrigeAndeler.snittAndeler(oppdaterteAndeler)
                .filter { it.stønadFom.isBefore(førsteEndring) } else forrigeAndeler
        return består.sortedBy { it.periodeId }
    }

    /**
     * Tar utgangspunkt i ny tilstand og finner andeler som må bygges opp (nye, endrede og bestående etter første endring)
     *
     * @param[oppdatertKjede] ny tilstand
     * @param[beståendeAndelerIKjeden] andeler man må bygge opp etter
     * @return andeler som må opprettes, hvis det ikke er noen beståendeAndeler returneres oppdatertKjede
     */
    fun andelerTilOpprettelse(oppdatertKjede: List<AndelTilkjentYtelse>,
                              beståendeAndelerIKjeden: List<AndelTilkjentYtelse>): List<AndelTilkjentYtelse> {

        return beståendeAndelerIKjeden.maxByOrNull { it.stønadTom }?.let { beståendeAndel ->
            oppdatertKjede.filter { it.stønadFom.isAfter(beståendeAndel.stønadTom) }
        } ?: oppdatertKjede
    }

    /**
     * Tar utgangspunkt i forrige tilstand og finner kjede med andeler til opphør og tilhørende opphørsdato
     *
     * @param[forrigeKjede] forrige behandlings tilstand
     * @param[oppdaterteKjede] nåværende tilstand
     * @return siste andel og opphørsdato fra kjede med opphør, returnerer null hvis det ikke finnes ett opphørsdato
     */
    fun andelTilOpphørMedDato(forrigeKjede: List<AndelTilkjentYtelse>,
                              oppdaterteKjede: List<AndelTilkjentYtelse>)
            : Pair<AndelTilkjentYtelse, LocalDate>? {

        val forrigeMaksDato = forrigeKjede.map { it.stønadTom }.maxOrNull()
        val forrigeAndeler = forrigeKjede.toSet()
        val oppdaterteAndeler = oppdaterteKjede.toSet()
        val førsteEndring = forrigeAndeler
                .disjunkteAndeler(oppdaterteAndeler).minByOrNull { it.stønadFom }?.stønadFom

        val sisteForrigeAndel = forrigeKjede.lastOrNull()
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

