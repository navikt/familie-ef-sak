package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakstype
import java.time.LocalDate
import java.time.LocalDateTime

object InfotrygdPeriodeTestUtil {

    fun lagInfotrygdPeriode(personIdent: String = "1",
                            stønadFom: LocalDate = LocalDate.now(),
                            stønadTom: LocalDate = LocalDate.now().plusDays(1),
                            opphørsdato: LocalDate? = null,
                            stønadId: Int = 1,
                            vedtakId: Int = 1,
                            beløp: Int = 1,
                            inntektsgrunnlag: Int = 1,
                            inntektsreduksjon: Int = 1,
                            samordningsfradrag: Int = 1,
                            kode: InfotrygdEndringKode = InfotrygdEndringKode.NY,
                            sakstype: InfotrygdSakstype = InfotrygdSakstype.SØKNAD): InfotrygdPeriode {
        return InfotrygdPeriode(personIdent = personIdent,
                                kode = kode,
                                sakstype = sakstype,
                                brukerId = "k40123",
                                stønadId = stønadId.toLong(),
                                vedtakId = vedtakId.toLong(),
                                stønadBeløp = beløp,
                                inntektsgrunnlag = inntektsgrunnlag,
                                inntektsreduksjon = inntektsreduksjon,
                                samordningsfradrag = samordningsfradrag,
                                beløp = beløp,
                                startDato = LocalDate.now(),
                                vedtakstidspunkt = LocalDateTime.now(),
                                stønadFom = stønadFom,
                                stønadTom = stønadTom,
                                opphørsdato = opphørsdato)
    }
}