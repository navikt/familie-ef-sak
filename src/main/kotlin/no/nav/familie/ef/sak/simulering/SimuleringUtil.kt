package no.nav.familie.ef.sak.simulering

import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import java.math.BigDecimal

fun Simuleringsoppsummering.hentSammenhengendePerioderMedFeilutbetaling(): List<Periode> {
    val perioderMedFeilutbetaling =
            perioder.sortedBy { it.fom }.filter { it.feilutbetaling > BigDecimal(0) }.map {
                Periode(it.fom, it.tom)
            }

    return perioderMedFeilutbetaling.fold(mutableListOf()) { akkumulatorListe, nestePeriode ->
        val gjeldendePeriode = akkumulatorListe.lastOrNull()

        if (gjeldendePeriode != null && erPerioderSammenhengende(gjeldendePeriode, nestePeriode)) {
            val oppdatertGjeldendePeriode = Periode(fom = gjeldendePeriode.fom,tom=nestePeriode.tom)
            akkumulatorListe.removeLast()
            akkumulatorListe.add(oppdatertGjeldendePeriode)
        } else {
            akkumulatorListe.add(nestePeriode)
        }
        akkumulatorListe
    }
}
private fun erPerioderSammenhengende(gjeldendePeriode: Periode, nestePeriode: Periode) =
        gjeldendePeriode.tom.plusDays(1) == nestePeriode.fom
