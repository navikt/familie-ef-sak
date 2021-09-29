package no.nav.familie.ef.sak.infotrygd

object InfotrygdPeriodeUtil {


    fun lagPerioder(perioderFraInfotrygd: List<InfotrygdPeriode>): List<InfotrygdPeriode> {

        val perioder = perioderFraInfotrygd.toSet()
            .map {
                if(it.opphørsdato != null) {
                    if(it.opphørsdato.isBefore(it.stønadTom)) {
                        return@map it.copy(stønadTom = it.opphørsdato)
                    }
                }
                it
            }
            .filter { it.stønadTom > it.stønadFom } // Skal infotrygd rydde bort disse? (inkl de der opphørdato er før startdato)
            .sortedWith(compareBy({ it.stønadId }, { it.vedtakId }))

        //val pairs = perioder.windowed(2, partialWindows = true)
        val pairs = perioder.zipWithNext()
        for (pair in pairs) {
            if (pair.first.erInfotrygdPeriodeOverlappende(pair.second)) {
                pair.first.stønadTom = pair.second.stønadFom.minusDays(1)
            }
        }


        // (a, b), (b, c), (c, null)

        return pairs.flatMap { it.toList() }.distinct()
    }
}