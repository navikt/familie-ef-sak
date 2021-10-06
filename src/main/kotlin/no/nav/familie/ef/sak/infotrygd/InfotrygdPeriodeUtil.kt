package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode


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
                .map {
                    val opphørsdato = it.opphørsdato
                    if (opphørsdato != null && opphørsdato.isBefore(it.stønadTom)) {
                        return@map it.copy(stønadTom = opphørsdato)
                    }
                    it
                }
                .filter { it.stønadTom > it.stønadFom } // Skal infotrygd rydde bort disse? (inkl de der opphørdato er før startdato)
                .sortedWith(compareBy<InfotrygdPeriode>({ it.stønadId }, { it.vedtakId }, { it.stønadFom }).reversed())
    }

}