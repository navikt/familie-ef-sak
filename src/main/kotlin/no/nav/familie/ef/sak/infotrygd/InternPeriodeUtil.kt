package no.nav.familie.ef.sak.infotrygd

object InternPeriodeUtil {

    /**
     * Slår sammen perioder tvers kilder
     * Når vi skal slå sammen perioder fra infotrygd og EF så er det EF sine perioder som er de som skriver over infotrygd sine
     */
    fun slåSammenPerioder(efPerioder: List<InternPeriode>, infotrygdperioder: List<InternPeriode>): List<InternPeriode> {
        val førstePerioden = efPerioder.minByOrNull { it.stønadFom } ?: return infotrygdperioder
        val perioderFraInfotrygdSomBeholdes = infotrygdperioder.mapNotNull {
            if (it.stønadFom >= førstePerioden.stønadFom) {
                null
            } else {
                it.copy(stønadTom = førstePerioden.stønadFom.minusDays(1))
            }
        }
        return efPerioder + perioderFraInfotrygdSomBeholdes
    }
}