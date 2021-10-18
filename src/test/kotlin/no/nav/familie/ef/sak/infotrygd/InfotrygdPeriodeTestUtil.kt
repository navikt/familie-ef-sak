package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import java.time.LocalDate

object InfotrygdPeriodeTestUtil {

    fun lagInfotrygdPeriode(personIdent: String = "1",
                            stønadFom: LocalDate = LocalDate.now(),
                            stønadTom: LocalDate = LocalDate.now().plusDays(1),
                            opphørdato: LocalDate? = null,
                            stønadId: Int = 1,
                            vedtakId: Int = 1,
                            beløp: Int = 1,
                            kode: InfotrygdEndringKode = InfotrygdEndringKode.NY): InfotrygdPeriode {
        return InfotrygdPeriode(personIdent = personIdent,
                                kode = kode,
                                brukerId = "",
                                stønadId = stønadId.toLong(),
                                vedtakId = vedtakId.toLong(),
                                stønadBeløp = 0,
                                inntektsreduksjon = 0,
                                samordningsfradrag = 0,
                                beløp = beløp,
                                startDato = LocalDate.now(),
                                stønadFom = stønadFom,
                                stønadTom = stønadTom,
                                opphørsdato = opphørdato)
    }
}