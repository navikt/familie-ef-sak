package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.dummy.FLYTTET_TIL_EF_IVERKSETT
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse.Companion.disjunkteAndeler
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse.Companion.snittAndeler
import java.time.LocalDate
import java.util.*

data class KjedeId(val klassifisering: String, val personIdent: String)

@Deprecated(FLYTTET_TIL_EF_IVERKSETT)
data class PeriodeId(val gjeldende: Long?,
                     val forrige: Long? = null)

@Deprecated(FLYTTET_TIL_EF_IVERKSETT)
fun AndelTilkjentYtelse.tilPeriodeId(): PeriodeId = PeriodeId(this.periodeId, this.forrigePeriodeId)

@Deprecated("Bør erstattes med å gjøre 'stønadFom' og  'stønadTom'  nullable")
val NULL_DATO: LocalDate = LocalDate.MIN

@Deprecated(FLYTTET_TIL_EF_IVERKSETT)
fun nullAndelTilkjentYtelse(behandlingId: UUID, personIdent: String, periodeId: PeriodeId?): AndelTilkjentYtelse =
        AndelTilkjentYtelse(beløp = 0,
                            stønadFom = NULL_DATO,
                            stønadTom = NULL_DATO,
                            personIdent = personIdent,
                            periodeId = periodeId?.gjeldende,
                            kildeBehandlingId = behandlingId,
                            forrigePeriodeId = periodeId?.forrige,
                            inntekt = 0,
                            inntektsreduksjon = 0,
                            samordningsfradrag = 0)

@Deprecated(FLYTTET_TIL_EF_IVERKSETT)
object ØkonomiUtils {

    /**
     * Lager oversikt over siste andel i hver kjede som finnes uten endring i oppdatert tilstand.
     * Vi må opphøre og eventuelt gjenoppbygge hver kjede etter denne. Må ta vare på andel og ikke kun offset da
     * filtrering av oppdaterte andeler senere skjer før offset blir satt.
     *
     * @param[andelerForrigeTilkjentYtelse] forrige behandlings tilstand
     * @param[andelerNyTilkjentYtelse] nåværende tilstand
     * @return liste med bestående andeler
     */
    fun beståendeAndeler(andelerForrigeTilkjentYtelse: List<AndelTilkjentYtelse>,
                         andelerNyTilkjentYtelse: List<AndelTilkjentYtelse>): List<AndelTilkjentYtelse> {
        val forrigeAndeler = andelerForrigeTilkjentYtelse.toSet()
        val oppdaterteAndeler = andelerNyTilkjentYtelse.toSet()

        val førsteEndring = finnDatoForFørsteEndredeAndel(forrigeAndeler, oppdaterteAndeler)
        val består =
                if (førsteEndring != null)
                    forrigeAndeler.snittAndeler(oppdaterteAndeler).filter { it.stønadFom.isBefore(førsteEndring) }
                else forrigeAndeler
        return består.sortedBy { it.periodeId }
    }

    /**
     * Tar utgangspunkt i ny tilstand og finner andeler som må bygges opp (nye, endrede og bestående etter første endring)
     *
     * @param[andelerNyTilkjentYtelse] ny tilstand
     * @param[beståendeAndeler] andeler man må bygge opp etter
     * @return andeler som må opprettes, hvis det ikke er noen beståendeAndeler returneres oppdatertKjede
     */
    fun andelerTilOpprettelse(andelerNyTilkjentYtelse: List<AndelTilkjentYtelse>,
                              beståendeAndeler: List<AndelTilkjentYtelse>): List<AndelTilkjentYtelse> {
        return beståendeAndeler.maxByOrNull { it.stønadTom }?.let { sisteBeståendeAndel ->
            andelerNyTilkjentYtelse.filter { it.stønadFom.isAfter(sisteBeståendeAndel.stønadTom) }
        } ?: andelerNyTilkjentYtelse
    }

    /**
     * Tar utgangspunkt i forrige tilstand og finner kjede med andeler til opphør og tilhørende opphørsdato
     *
     * @param[andelerForrigeTilkjentYtelse] forrige behandlings tilstand
     * @param[andelerNyTilkjentYtelse] nåværende tilstand
     * @return siste andel og opphørsdato fra kjede med opphør, returnerer null hvis det ikke finnes ett opphørsdato
     */
    fun andelTilOpphørMedDato(andelerForrigeTilkjentYtelse: List<AndelTilkjentYtelse>,
                              andelerNyTilkjentYtelse: List<AndelTilkjentYtelse>): Pair<AndelTilkjentYtelse, LocalDate>? {

        val forrigeMaksDato = andelerForrigeTilkjentYtelse.map { it.stønadTom }.maxOrNull()
        val forrigeAndeler = andelerForrigeTilkjentYtelse.toSet()
        val oppdaterteAndeler = andelerNyTilkjentYtelse.toSet()
        val førsteEndring = finnDatoForFørsteEndredeAndel(forrigeAndeler, oppdaterteAndeler)

        val sisteForrigeAndel = andelerForrigeTilkjentYtelse.lastOrNull()
        return if (sisteForrigeAndel == null || førsteEndring == null || erNyPeriode(forrigeMaksDato, førsteEndring)) {
            null
        } else {
            Pair(sisteForrigeAndel, førsteEndring)
        }
    }

    private fun finnDatoForFørsteEndredeAndel(andelerForrigeTilkjentYtelse: Set<AndelTilkjentYtelse>,
                                              andelerNyTilkjentYtelse: Set<AndelTilkjentYtelse>) =
            andelerForrigeTilkjentYtelse.disjunkteAndeler(andelerNyTilkjentYtelse).minByOrNull { it.stønadFom }?.stønadFom

    /**
     * Sjekker om den nye endringen er etter maks datot for tidligere perioder
     */
    private fun erNyPeriode(forrigeMaksDato: LocalDate?, førsteEndring: LocalDate) =
            forrigeMaksDato != null && førsteEndring.isAfter(forrigeMaksDato)

}

