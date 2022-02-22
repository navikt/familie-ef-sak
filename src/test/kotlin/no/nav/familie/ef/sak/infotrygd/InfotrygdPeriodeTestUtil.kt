package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdAktivitetstype
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdOvergangsstønadKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

object InfotrygdPeriodeTestUtil {

    fun lagInfotrygdPeriode(personIdent: String = "1",
                            stønadFom: LocalDate = YearMonth.now().atDay(1),
                            stønadTom: LocalDate = YearMonth.now().atEndOfMonth(),
                            opphørsdato: LocalDate? = null,
                            stønadId: Int = 1,
                            vedtakId: Int = 1,
                            beløp: Int = 1,
                            inntektsgrunnlag: Int = 1,
                            inntektsreduksjon: Int = 1,
                            samordningsfradrag: Int = 1,
                            kode: InfotrygdEndringKode = InfotrygdEndringKode.NY,
                            sakstype: InfotrygdSakstype = InfotrygdSakstype.SØKNAD,
                            aktivitetstype: InfotrygdAktivitetstype? = InfotrygdAktivitetstype.BRUKERKONTAKT,
                            kodeOvergangsstønad: InfotrygdOvergangsstønadKode? = InfotrygdOvergangsstønadKode.BARN_UNDER_1_3_ÅR
    ): InfotrygdPeriode {
        return InfotrygdPeriode(personIdent = personIdent,
                                kode = kode,
                                sakstype = sakstype,
                                kodeOvergangsstønad = kodeOvergangsstønad,
                                aktivitetstype = aktivitetstype,
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