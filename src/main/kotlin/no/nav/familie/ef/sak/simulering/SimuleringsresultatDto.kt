package no.nav.familie.ef.sak.simulering

import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.familie.kontrakter.felles.simulering.Simuleringsperiode
import java.math.BigDecimal
import java.time.LocalDate

data class SimuleringsresultatDto(
        val perioder: List<SimuleringsPeriode>,
        val fomDatoNestePeriode: LocalDate?,
        val etterbetaling: BigDecimal,
        val feilutbetaling: BigDecimal,
        val fom: LocalDate?,
        val tomDatoNestePeriode: LocalDate?,
        val forfallsdatoNestePeriode: LocalDate?,
        val tidSimuleringHentet: LocalDate?,
        val tomSisteUtbetaling: LocalDate?,
) {

    fun hentSammenhengendePerioderMedFeilutbetaling(): List<Periode> {
        val perioderMedFeilutbetaling =
                perioder.sortedBy { it.fom }.filter { it.feilutbetaling > BigDecimal(0) }.map { Periode(it.fom, it.tom) }

        return perioderMedFeilutbetaling.fold(mutableListOf()) { akkumulatorListe, nestePeriode ->
            val gjeldendePeriode = akkumulatorListe.lastOrNull()

            if (gjeldendePeriode != null && erPerioderSammenhengende(gjeldendePeriode, nestePeriode)) {
                val oppdatertGjeldendePeriode = gjeldendePeriode.copy(tom = nestePeriode.tom)
                akkumulatorListe.removeLast()
                akkumulatorListe.add(oppdatertGjeldendePeriode)
            } else {
                akkumulatorListe.add(nestePeriode)
            }
            akkumulatorListe
        }
    }

    private fun erPerioderSammenhengende(gjeldendePeriode: Periode,
                                         nestePeriode: Periode) = gjeldendePeriode.tom.plusDays(1) == nestePeriode.fom

}

data class Periode(
        val fom: LocalDate,
        val tom: LocalDate)

data class SimuleringsPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val forfallsdato: LocalDate,
        val nyttBeløp: BigDecimal,
        val tidligereUtbetalt: BigDecimal,
        val resultat: BigDecimal,
        val feilutbetaling: BigDecimal,
)

fun SimuleringsresultatDto.tilFelles(): Simuleringsoppsummering =
        Simuleringsoppsummering(
                perioder = this.perioder.map { it.tilFelles() },
                fomDatoNestePeriode = this.fomDatoNestePeriode,
                etterbetaling = this.etterbetaling,
                feilutbetaling = this.feilutbetaling,
                fom = this.fom,
                tomDatoNestePeriode = this.tomDatoNestePeriode,
                forfallsdatoNestePeriode = this.forfallsdatoNestePeriode,
                tidSimuleringHentet = this.tidSimuleringHentet,
                tomSisteUtbetaling = this.tomSisteUtbetaling
        )

fun SimuleringsPeriode.tilFelles(): Simuleringsperiode =
        Simuleringsperiode(
                fom = this.fom,
                tom = this.tom,
                forfallsdato = this.forfallsdato,
                nyttBeløp = this.nyttBeløp,
                tidligereUtbetalt = this.tidligereUtbetalt,
                resultat = this.resultat,
                feilutbetaling = this.feilutbetaling
        )
