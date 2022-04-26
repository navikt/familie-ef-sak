package no.nav.familie.ef.sak.infotrygd

object InternPeriodeUtil {

    /**
     * Slår sammen perioder tvers kilder
     * Når vi skal slå sammen perioder fra infotrygd og EF så er det EF sine perioder som er de som skriver over infotrygd sine
     */
    fun slåSammenPerioder(efPerioder: EfInternPerioder?,
                          infotrygdperioder: List<InternPeriode>): List<InternPeriode> {
        val startdato = efPerioder?.startdato ?: return infotrygdperioder
        val perioderFraInfotrygdSomBeholdes = infotrygdperioder.mapNotNull {
            if (it.stønadFom >= startdato) {
                null
            } else if (it.stønadTom > startdato) {
                it.copy(stønadTom = startdato.minusDays(1))
            } else {
                it
            }
        }
        return efPerioder.internperioder + perioderFraInfotrygdSomBeholdes
    }
}