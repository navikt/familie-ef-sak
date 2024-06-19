package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.felles.util.DatoUtil
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdAktivitetstype
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdOvergangsstønadKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakstype
import java.time.LocalDate

object InfotrygdPeriodeTestUtil {
    fun lagInfotrygdPeriode(
        personIdent: String = "1",
        stønadFom: LocalDate = DatoUtil.årMånedNå().atDay(1),
        stønadTom: LocalDate = DatoUtil.årMånedNå().atEndOfMonth(),
        opphørsdato: LocalDate? = null,
        stønadId: Int = 1,
        vedtakId: Int = 1,
        beløp: Int = 1,
        inntektsgrunnlag: Int = 1,
        inntektsreduksjon: Int = 1,
        samordningsfradrag: Int = 1,
        utgifterBarnetilsyn: Int = 1,
        kode: InfotrygdEndringKode = InfotrygdEndringKode.NY,
        sakstype: InfotrygdSakstype = InfotrygdSakstype.SØKNAD,
        aktivitetstype: InfotrygdAktivitetstype? = InfotrygdAktivitetstype.BRUKERKONTAKT,
        kodeOvergangsstønad: InfotrygdOvergangsstønadKode? = InfotrygdOvergangsstønadKode.BARN_UNDER_1_3_ÅR,
        barnIdenter: List<String> = emptyList(),
    ): InfotrygdPeriode =
        InfotrygdPeriode(
            personIdent = personIdent,
            kode = kode,
            sakstype = sakstype,
            kodeOvergangsstønad = kodeOvergangsstønad,
            aktivitetstype = aktivitetstype,
            brukerId = "k40123",
            stønadId = stønadId.toLong(),
            vedtakId = vedtakId.toLong(),
            inntektsgrunnlag = inntektsgrunnlag,
            inntektsreduksjon = inntektsreduksjon,
            samordningsfradrag = samordningsfradrag,
            utgifterBarnetilsyn = utgifterBarnetilsyn,
            månedsbeløp = beløp,
            engangsbeløp = beløp,
            startDato = DatoUtil.dagensDato(),
            vedtakstidspunkt = DatoUtil.dagensDatoMedTid(),
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            opphørsdato = opphørsdato,
            barnIdenter = barnIdenter,
        )
}
