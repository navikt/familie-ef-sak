package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.felles.util.isEqualOrAfter

object InfotrygdPeriodeUtil {


    fun lagPerioder(perioderFraInfotrygd: List<InfotrygdPeriode>): List<InfotrygdPeriode> {

        val perioder = perioderFraInfotrygd.toSet()
            .map {
                if (it.opphørsdato != null) {
                    if (it.opphørsdato.isBefore(it.stønadTom)) {
                        return@map it.copy(stønadTom = it.opphørsdato)
                    }
                }
                it
            }
            .filter { it.stønadTom > it.stønadFom } // Skal infotrygd rydde bort disse? (inkl de der opphørdato er før startdato)
            .sortedWith(compareBy<InfotrygdPeriode>({ it.stønadId }, { it.vedtakId }, { it.stønadFom }).reversed())

        val list = mutableListOf<InfotrygdPeriode>()

        for (periode in perioder) {
            val match = list.filter { it.erInfotrygdPeriodeOverlappende(periode) }.minByOrNull { it.stønadFom }
            if (match != null && periode.stønadFom.isEqualOrAfter(match.stønadFom)) {
                continue
            }

            list.add(periode.copy(stønadTom = match?.stønadFom?.minusDays(1) ?: periode.stønadTom))
        }
        return list.reversed()
    }
}