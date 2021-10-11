package no.nav.familie.ef.sak.no.nav.familie.ef.sak.simulering

import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object SimuleringsposteringTestUtil {

    fun lagPosteringer(fraDato: LocalDate,
                       antallMåneder: Int = 1,
                       beløp: BigDecimal = BigDecimal(5000),
                       posteringstype: PosteringType = PosteringType.YTELSE

    ): List<SimulertPostering> = MutableList(antallMåneder) { index ->
        SimulertPostering(fagOmrådeKode = FagOmrådeKode.ENSLIG_FORSØRGER_OVERGANGSSTØNAD,
                          fom = fraDato.plusMonths(index.toLong()),
                          tom = fraDato.plusMonths(index.toLong()).with(TemporalAdjusters.lastDayOfMonth()),
                          betalingType = BetalingType.DEBIT,
                          beløp = beløp,
                          posteringType = posteringstype,
                          forfallsdato = fraDato.plusMonths(index.toLong()).with(TemporalAdjusters.lastDayOfMonth()),
                          utenInntrekk = false)
    }
}