package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.ef.sak.beregning.BeregningUtils.TiProsentOppOgNed
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.felles.util.månedTilNorskFormat
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

data class FlettefelterForInntektsbegrunnelse(
    val forrigeForventetInnntektsperiodeFraOgMed: YearMonth,
    val kontrollperiodeTilOgMed: YearMonth = YearMonth.now().minusMonths(1),
    val førsteMånedMed10ProsentEndring: YearMonth,
    val beløpFørsteMåned10ProsentEndring: Int,
    val forrigeForventetÅrsinntekt: Int,
    val tiProsentOppOgNed: TiProsentOppOgNed,
    val forventetInntektFraMåned: YearMonth,
    val harFeriepenger: Boolean,
    val inntektsberegningGOmregning: InntektsberegningGOmregning,
) {
    val feriepengerTekst =
        if (harFeriepenger) {
            " Bruker har fått utbetalt feriepenger i løpet av siste tre måneder. Disse er ikke tatt med i beregningen av forventet inntekt."
        } else {
            ""
        }

    val genererInntektsbegrunnelse =
        """
        Periode som er kontrollert: ${forrigeForventetInnntektsperiodeFraOgMed.tilNorskFormat()} til ${kontrollperiodeTilOgMed.tilNorskFormat()}.
        
        Forventet årsinntekt i ${førsteMånedMed10ProsentEndring.tilNorskFormat()}: ${forrigeForventetÅrsinntekt.tilNorskFormat()} kroner.
        - 10 % opp: ${tiProsentOppOgNed.opp.tilNorskFormat()} kroner per måned.
        - 10 % ned: ${tiProsentOppOgNed.ned.tilNorskFormat()} kroner per måned.
        ${tekstTypeForGOmregningOppOgNed(inntektsberegningGOmregning.erForrigeBehandlingGOmregning, inntektsberegningGOmregning.forrigeForventetÅrsinntektG, inntektsberegningGOmregning.tiProsentOppOgNed, førsteMånedMed10ProsentEndring)}
        Inntekten i ${førsteMånedMed10ProsentEndring.tilNorskFormat()} er ${beløpFørsteMåned10ProsentEndring.tilNorskFormat()} kroner. Inntekten har økt minst 10 prosent denne måneden og alle månedene etter dette. Stønaden beregnes på nytt fra måneden etter 10 prosent økning.
        
        Har lagt til grunn faktisk inntekt bakover i tid. Fra og med ${forventetInntektFraMåned.tilNorskFormat()} er stønaden beregnet ut ifra gjennomsnittlig inntekt for ${forventetInntektFraMåned.minusMonths(3).månedTilNorskFormat()}, ${forventetInntektFraMåned.minusMonths(2).månedTilNorskFormat()} og ${forventetInntektFraMåned.minusMonths(1).månedTilNorskFormat()}.$feriepengerTekst
        
        A-inntekt er lagret.
        """.trimIndent()
}

data class InntektsberegningGOmregning(
    val erForrigeBehandlingGOmregning: Boolean,
    val forrigeForventetÅrsinntektG: Int,
    val tiProsentOppOgNed: TiProsentOppOgNed,
)

data class FlettefelterForInntektsbegrunnelseForNullVedtak(
    val førsteMånedMed10ProsentEndring: YearMonth,
    val forrigeForventetÅrsinntekt: Int,
    val beløpFørsteMåned10ProsentEndring: Int,
    val harFeriepenger: Boolean,
) {
    val feriepengerTekst =
        if (harFeriepenger) {
            " Bruker har fått utbetalt feriepenger i løpet av siste tre måneder. Disse er ikke tatt med i beregningen av forventet inntekt."
        } else {
            ""
        }

    val genererInntektsbegrunnelse =
        """
        Forventet årsinntekt i ${førsteMånedMed10ProsentEndring.tilNorskFormat()}: ${forrigeForventetÅrsinntekt.tilNorskFormat()} kroner.
            - Månedsinntekt 1/2 G: ${(Grunnbeløpsperioder.nyesteGrunnbeløp.perMnd.toInt() / 2).tilNorskFormat()} kroner
        
        Mottar uredusert stønad.
        
        Inntekten i ${førsteMånedMed10ProsentEndring.tilNorskFormat()} er ${beløpFørsteMåned10ProsentEndring.tilNorskFormat()} kroner. Bruker har inntekt over 1/2 G denne måneden og alle månedene etter dette.
        Stønaden beregnes på nytt fra måneden etter inntekten oversteg 1/2 G. $feriepengerTekst
        """.trimIndent().trimEnd()
}

fun Int.tilNorskFormat(): String {
    val formatter = NumberFormat.getInstance(Locale.forLanguageTag("no-NO"))
    return formatter.format(this)
}

fun tekstTypeForGOmregningOppOgNed(
    forrigeBehandlingGOmregning: Boolean,
    forrigeForventetÅrsinntektG: Int,
    tiProsentOppOgNedG: TiProsentOppOgNed,
    førsteMånedMed10ProsentEndring: YearMonth,
): String =
    if (forrigeBehandlingGOmregning && førsteMånedMed10ProsentEndring != Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fom) {
        """
        Forventet årsinntekt fra ${Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed.tilNorskFormat()}: ${forrigeForventetÅrsinntektG.tilNorskFormat()} kroner.
        - 10 % opp: ${tiProsentOppOgNedG.opp.tilNorskFormat()} kroner per måned.
        - 10 % ned: ${tiProsentOppOgNedG.ned.tilNorskFormat()} kroner per måned.
        """
    } else {
        ""
    }
