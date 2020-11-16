package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse.Companion.disjunkteAndeler
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse.Companion.snittAndeler
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import java.time.LocalDate

data class KjedeId(val klassifiering: String, val personIdent: String)

data class PeriodeId(val gjeldende: Long?,
                     val forrige: Long? = null)

fun AndelTilkjentYtelse.tilKjedeId(type: Stønadstype): KjedeId = KjedeId(type.tilKlassifisering(), this.personIdent)
fun AndelTilkjentYtelse.tilPeriodeId(): PeriodeId = PeriodeId(this.periodeId, this.forrigePeriodeId)

@Deprecated("Bør erstattes med å gjøre 'stønadFom' og  'stønadTom'  nullable")
val NULL_DATO: LocalDate = LocalDate.MIN

fun KjedeId.tilNullAndelTilkjentYtelse(periodeId: PeriodeId?): AndelTilkjentYtelse =
        AndelTilkjentYtelse(beløp = 0,
                            stønadFom = NULL_DATO,
                            stønadTom = NULL_DATO,
                            personIdent = this.personIdent,
                            periodeId = periodeId?.gjeldende,
                            forrigePeriodeId = periodeId?.forrige)

object ØkonomiUtils {

    /**
     * Lager oversikt over siste andel i hver kjede som finnes uten endring i oppdatert tilstand.
     * Vi må opphøre og eventuelt gjenoppbygge hver kjede etter denne. Må ta vare på andel og ikke kun offset da
     * filtrering av oppdaterte andeler senere skjer før offset blir satt.
     * Personident er identifikator for hver kjede, med unntak av småbarnstillegg som vil være en egen "person".
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return map med personident og siste bestående andel. Bestående andel=null dersom alle opphøres eller ny person.
     */
    fun beståendeAndelerPerKjede(forrigeKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>,
                                 oppdaterteKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>)
            : Map<KjedeId, List<AndelTilkjentYtelse>> {
        val alleKjedeIder = forrigeKjeder.keys.union(oppdaterteKjeder.keys)
        return alleKjedeIder.associateWith { kjedeId ->
            beståendeAndelerIKjede(forrigeKjede = forrigeKjeder[kjedeId],
                                   oppdatertKjede = oppdaterteKjeder[kjedeId])
        }
    }

    private fun beståendeAndelerIKjede(forrigeKjede: List<AndelTilkjentYtelse>?,
                                       oppdatertKjede: List<AndelTilkjentYtelse>?): List<AndelTilkjentYtelse> {
        val forrigeAndeler = forrigeKjede?.toSet() ?: emptySet()
        val oppdaterteAndeler = oppdatertKjede?.toSet() ?: emptySet()
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
    fun andelerTilOpprettelse(oppdaterteKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>,
                              beståendeAndelerIHverKjede: Map<KjedeId, List<AndelTilkjentYtelse>>)
            : Map<KjedeId, List<AndelTilkjentYtelse>> {

        val sisteBeståendeAndelIHverKjede =
                beståendeAndelerIHverKjede.map { (kjedeId, kjede) ->
                    Pair(kjedeId, kjede.sortedBy { it.periodeId }.lastOrNull())
                }.toMap()

        return oppdaterteKjeder.map { (kjedeId, oppdaterteAndeler) ->
            if (sisteBeståendeAndelIHverKjede[kjedeId] != null)
                Pair(kjedeId, oppdaterteAndeler
                        .filter { it.stønadFom.isAfter(sisteBeståendeAndelIHverKjede[kjedeId]!!.stønadTom) })
            else Pair(kjedeId, oppdaterteAndeler)
        }
                .toMap()
                .filter { (_, kjede) -> kjede.isNotEmpty() }
    }

    /**
     * Tar utgangspunkt i forrige tilstand og finner kjeder med andeler til opphør og tilhørende opphørsdato
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return map av siste andel og opphørsdato fra kjeder med opphør
     */
    fun andelTilOpphørMedDato(forrigeKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>,
                              oppdaterteKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>)
            : Map<KjedeId, Pair<AndelTilkjentYtelse, LocalDate>> {

        return forrigeKjeder.mapValues { (kjedeId, kjede) ->
            val forrigeAndeler = kjede.toSet()
            val oppdaterteAndeler = oppdaterteKjeder[kjedeId]?.toSet() ?: emptySet()
            val førsteEndring = forrigeAndeler
                    .disjunkteAndeler(oppdaterteAndeler).minByOrNull { it.stønadFom }?.stønadFom

            Pair(kjede.lastOrNull(), førsteEndring)
        }
                .filter { (_, pair) -> pair.first != null && pair.second != null }
                .mapValues { (_, pair) -> Pair(pair.first!!, pair.second!!) }

    }
}

