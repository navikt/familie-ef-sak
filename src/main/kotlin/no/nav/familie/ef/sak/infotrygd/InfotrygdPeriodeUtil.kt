package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.felles.util.isEqualOrBefore
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import java.time.LocalDate


/**
 * Før vi slår sammen perioder fra infotrygd så må vi sette TOM-dato til opphøret sitt dato hvis opphørsdatoet er før TOM-dato
 * Vi filtrerer også ut ugyldige perioder, der TOM er før FOM
 *
 * Før vi slår sammen, sorterer vi de i en rekkefølge som de ble lagt inn i infotrygd,
 * sånn att en senere periode overskrever en tidligere periode
 */
object InfotrygdPeriodeUtil {

    fun filtrerOgSorterPerioderFraInfotrygd(perioderFraInfotrygd: List<InfotrygdPeriode>): List<InfotrygdPeriode> {
        return perioderFraInfotrygd.toSet()
                .map { brukOpphørsdatoSomTomHvisDenFinnes(it) }
                .filter { it.stønadTom > it.stønadFom } // Skal infotrygd rydde bort disse? (inkl de der opphørdato er før startdato)
                .sortedWith(compareBy<InfotrygdPeriode>({ it.stønadId }, { it.vedtakId }, { it.stønadFom }).reversed())
    }

    private fun brukOpphørsdatoSomTomHvisDenFinnes(it: InfotrygdPeriode): InfotrygdPeriode {
        val opphørsdato = it.opphørsdato
        return if (opphørsdato != null && opphørsdato.isBefore(it.stønadTom)) {
            it.copy(stønadTom = trekkFraEnDagHvisFørsteIMåneden(opphørsdato))
        } else {
            it
        }
    }

    /**
     * Noen opphørsdatoer kommer som eks 01.02.yyyy, då skal vi bruke 31.01.yyyy
     */
    private fun trekkFraEnDagHvisFørsteIMåneden(opphørsdato: LocalDate) =
            if (opphørsdato.dayOfMonth == 1) opphørsdato.minusDays(1) else opphørsdato

    /**
     * Slår sammen perioder fra infotrygd, disse skal ikke slås sammen tvers ulike stønadId'er
     */
    fun slåSammenInfotrygdperioder(infotrygdperioder: List<InfotrygdPeriode>): List<InfotrygdPeriode> {
        return filtrerOgSorterPerioderFraInfotrygd(infotrygdperioder)
                .filter { it.kode != InfotrygdEndringKode.ANNULERT && it.kode != InfotrygdEndringKode.UAKTUELL }
                .groupBy { it.stønadId }
                .values
                .flatMap(this::slåSammenPerioder)
                .sortedByDescending { it.stønadFom }
    }

    private fun slåSammenPerioder(perioder: List<InfotrygdPeriode>): MutableList<InfotrygdPeriode> {
        val list = mutableListOf<InfotrygdPeriode>()

        for (periode in perioder) {
            val minStønadFom = list.minByOrNull { it.stønadFom }
            if (minStønadFom != null && periode.stønadFom.isEqualOrAfter(minStønadFom.stønadFom)) {
                continue
            } else if (minStønadFom != null && minStønadFom.erPeriodeOverlappende(periode)) {
                list.add(periode.copy(stønadTom = minStønadFom.stønadFom.minusDays(1)))
            } else {
                list.add(periode)
            }
        }
        return list
    }

    private fun InfotrygdPeriode.erDatoInnenforPeriode(dato: LocalDate): Boolean {
        return dato.isEqualOrBefore(stønadTom) && dato.isEqualOrAfter(stønadFom)
    }

    fun InfotrygdPeriode.erPeriodeOverlappende(periode: InfotrygdPeriode): Boolean {
        return (erDatoInnenforPeriode(periode.stønadFom) || erDatoInnenforPeriode(periode.stønadTom))
               || omslutter(periode)
    }

    private fun InfotrygdPeriode.omslutter(periode: InfotrygdPeriode) =
            periode.stønadFom.isBefore(stønadFom) && periode.stønadTom.isAfter(stønadTom)

}