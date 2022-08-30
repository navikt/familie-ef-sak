package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.infotrygd.InternPeriode
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import java.time.LocalDate

object InternPeriodeTestUtil {

    fun lagInternPeriode(
        inntektsreduksjon: Int = 0,
        samordningsfradrag: Int = 0,
        utgifterBarnetilsyn: Int = 0,
        beløp: Int = 0,
        stønadFom: LocalDate = LocalDate.now(),
        stønadTom: LocalDate = LocalDate.now().plusDays(1),
        opphørdato: LocalDate? = null,
        datakilde: PeriodeOvergangsstønad.Datakilde = PeriodeOvergangsstønad.Datakilde.INFOTRYGD
    ): InternPeriode {
        return InternPeriode(
            personIdent = "1",
            inntektsreduksjon = inntektsreduksjon,
            samordningsfradrag = samordningsfradrag,
            utgifterBarnetilsyn = utgifterBarnetilsyn,
            månedsbeløp = beløp,
            engangsbeløp = beløp,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            opphørsdato = opphørdato,
            datakilde = datakilde
        )
    }
}
