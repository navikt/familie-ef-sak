package no.nav.familie.ef.sak.infotrygd

import java.time.LocalDate

data class InfotrygdPeriode(
        val stønadId: Long,
        val vedtakId: Long,
        //val stonadBelop: Int,
        val inntektsreduksjon: Int,
        val samordningsfradrag: Int,
        val beløp: Int, // netto_belop
        //val datoStart: String, // usikker om vi trenger denne
        val stønadFom: LocalDate,
        val stønadTom: LocalDate,
        val datoOpphor: LocalDate?
) {

    fun erDatoInnenforPeriode(dato: LocalDate): Boolean {
        return dato.isEqualOrBefore(stønadTom) && dato.isEqualOrAfter(stønadFom)
    }

    fun erInfotrygdPeriodeOverlappende(infotrygdPeriode: InfotrygdPeriode): Boolean {
        return (erDatoInnenforPeriode(infotrygdPeriode.stønadFom) || erDatoInnenforPeriode(infotrygdPeriode.stønadTom))
               || omslutter(infotrygdPeriode)
    }

    private fun omslutter(infotrygdPeriode: InfotrygdPeriode) =
            infotrygdPeriode.stønadFom.isBefore(stønadFom) && infotrygdPeriode.stønadTom.isAfter(stønadTom)
}