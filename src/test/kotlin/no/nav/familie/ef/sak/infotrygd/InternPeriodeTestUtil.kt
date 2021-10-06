package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.infotrygd.InternPeriode
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import java.time.LocalDate

object InternPeriodeTestUtil {

    fun lagInternPeriode(inntektsreduksjon: Int = 0,
                         samordningsfradrag: Int = 0,
                         beløp: Int = 0,
                         stønadFom: LocalDate = LocalDate.now(),
                         stønadTom: LocalDate = LocalDate.now().plusDays(1),
                         opphørdato: LocalDate? = null): InternPeriode {
        return InternPeriode(
                inntektsreduksjon = inntektsreduksjon,
                samordningsfradrag = samordningsfradrag,
                beløp = beløp,
                stønadFom = stønadFom,
                stønadTom = stønadTom,
                opphørsdato = opphørdato,
                datakilde = PeriodeOvergangsstønad.Datakilde.EF)
    }
}