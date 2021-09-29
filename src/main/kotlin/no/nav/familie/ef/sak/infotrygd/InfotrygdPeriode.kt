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
)