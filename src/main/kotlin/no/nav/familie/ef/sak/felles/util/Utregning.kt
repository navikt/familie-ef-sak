package no.nav.familie.ef.sak.felles.util

import java.math.BigDecimal
import java.math.RoundingMode

object Utregning {

    fun rundNedTilNærmeste100(beløp: BigDecimal): BigDecimal {
        val beløpSomHeltal = beløp.setScale(0, RoundingMode.FLOOR).toLong()
        return ((beløpSomHeltal / 100L) * 100L).toBigDecimal()
    }

    fun rundNedTilNærmeste1000(beløp: BigDecimal): Long {
        val beløpSomHeltal = beløp.setScale(0, RoundingMode.FLOOR).toLong()
        return (beløpSomHeltal / 1000L) * 1000L
    }
}
