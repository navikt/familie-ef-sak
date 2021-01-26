package no.nav.familie.ef.sak.api.beregning

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class BeregningService {

    fun beregnFullYtelse(beregningRequest: BeregningRequest): List<Beløpsperiode> {
        return finnGrunnbeløpsPerioder(beregningRequest.stønadFom, beregningRequest.stønadTom).map {
            Beløpsperiode(it.fraOgMedDato,
                          it.tilDato,
                          it.beløp.multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_UP))
        }
    }
}