package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.felles.util.isEqualOrAfter

object InternPeriodeUtil {
    /**
     * Slår sammen perioder tvers kilder, viktig att EF-perioder
     */
    fun slåSammenPerioder(perioder: List<InternPeriode>): List<InternPeriode> {
        val list = mutableListOf<InternPeriode>()

        for (periode in perioder) {
            val match = list.filter { it.erPeriodeOverlappende(periode) }.minByOrNull { it.stønadFom }
            if (match != null && periode.stønadFom.isEqualOrAfter(match.stønadFom)) {
                continue
            }

            list.add(periode.copy(stønadTom = match?.stønadFom?.minusDays(1) ?: periode.stønadTom))
        }
        return list.reversed()
    }
}