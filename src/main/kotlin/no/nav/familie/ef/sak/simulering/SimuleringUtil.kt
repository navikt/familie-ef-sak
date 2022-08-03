package no.nav.familie.ef.sak.simulering

import no.nav.familie.kontrakter.felles.Periode
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import java.math.BigDecimal

fun Simuleringsoppsummering.hentSammenhengendePerioderMedFeilutbetaling(): List<Periode> {
    val perioderMedFeilutbetaling =
        perioder.sortedBy { it.fom }.filter { it.feilutbetaling > BigDecimal(0) }.map {
            Periode(it.fom, it.tom)
        }

    return perioderMedFeilutbetaling.fold(mutableListOf()) { akkumulatorListe, nestePeriode ->
        val gjeldendePeriode = akkumulatorListe.lastOrNull()

        if (gjeldendePeriode != null && erPerioderSammenhengende(gjeldendePeriode, nestePeriode)) {
            val oppdatertGjeldendePeriode = gjeldendePeriode union nestePeriode
            akkumulatorListe.removeLast()
            akkumulatorListe.add(oppdatertGjeldendePeriode)
        } else {
            akkumulatorListe.add(nestePeriode)
        }
        akkumulatorListe
    }
}
private fun erPerioderSammenhengende(gjeldendePeriode: Periode, nestePeriode: Periode) =
    gjeldendePeriode påfølgesAv nestePeriode
