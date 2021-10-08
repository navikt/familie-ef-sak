package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import java.time.LocalDate

object InfotrygdPeriodeUtil {

    fun lagInfotrygdPeriode(personIdent: String,
                            kode: InfotrygdEndringKode = InfotrygdEndringKode.NY): InfotrygdPeriode {
        return InfotrygdPeriode(personIdent = personIdent,
                                kode = kode,
                                brukerId = "",
                                stønadId = 1,
                                vedtakId = 1,
                                stønadBeløp = 0,
                                inntektsreduksjon = 0,
                                samordningsfradrag = 0,
                                beløp = 0,
                                startDato = LocalDate.now(),
                                stønadFom = LocalDate.now(),
                                stønadTom = LocalDate.now(),
                                opphørsdato = LocalDate.now())
    }
}