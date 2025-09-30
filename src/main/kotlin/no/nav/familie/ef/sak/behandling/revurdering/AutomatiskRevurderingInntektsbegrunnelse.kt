package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.ef.sak.beregning.BeregningUtils.TiProsentOppOgNed
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.felles.util.månedTilNorskFormat
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

data class FlettefelterForInntektsbegrunnelse(
    val kontrollperiodeFraOgMed: YearMonth,
    val kontrollperiodeTilOgMed: YearMonth = YearMonth.now().minusMonths(1),
    val førsteMånedMed10ProsentEndring: YearMonth,
    val månedsinntektFørsteMåned10ProsentEndring: Int,
    val forventetÅrsinntektNår10ProsentEndring: Int,
    val tiProsentOppOgNedFraForventetÅrsinntekt: TiProsentOppOgNed,
    val nyForventetInntektFraOgMedDato: YearMonth,
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
        Periode som er kontrollert: ${kontrollperiodeFraOgMed.tilNorskFormat()} til ${kontrollperiodeTilOgMed.tilNorskFormat()}.
        
        Forventet årsinntekt i ${førsteMånedMed10ProsentEndring.tilNorskFormat()}: ${forventetÅrsinntektNår10ProsentEndring.tilNorskFormat()} kroner.
        - 10 % opp: ${tiProsentOppOgNedFraForventetÅrsinntekt.opp.tilNorskFormat()} kroner per måned.
        - 10 % ned: ${tiProsentOppOgNedFraForventetÅrsinntekt.ned.tilNorskFormat()} kroner per måned.
        ${tekstTypeForGOmregningOppOgNed(inntektsberegningGOmregning.erForrigeBehandlingGOmregning, inntektsberegningGOmregning.forrigeForventetÅrsinntektG, inntektsberegningGOmregning.tiProsentOppOgNed, førsteMånedMed10ProsentEndring)}
        Inntekten i ${førsteMånedMed10ProsentEndring.tilNorskFormat()} er ${månedsinntektFørsteMåned10ProsentEndring.tilNorskFormat()} kroner. Inntekten har økt minst 10 prosent denne måneden og alle månedene etter dette. Stønaden beregnes på nytt fra måneden etter 10 prosent økning.
        
        Har lagt til grunn faktisk inntekt bakover i tid. Fra og med ${nyForventetInntektFraOgMedDato.tilNorskFormat()} er stønaden beregnet ut ifra gjennomsnittlig inntekt for ${nyForventetInntektFraOgMedDato.minusMonths(3).månedTilNorskFormat()}, ${nyForventetInntektFraOgMedDato.minusMonths(2).månedTilNorskFormat()} og ${nyForventetInntektFraOgMedDato.minusMonths(1).månedTilNorskFormat()}.$feriepengerTekst
        
        A-inntekt er lagret.
        """.trimIndent()
}

data class InntektsberegningGOmregning(
    val erForrigeBehandlingGOmregning: Boolean,
    val forrigeForventetÅrsinntektG: Int,
    val tiProsentOppOgNed: TiProsentOppOgNed,
)

data class FlettefelterForInntektsbegrunnelseForInntektUnderHalvG(
    val førsteMånedMed10ProsentEndring: YearMonth,
    val forventetÅrsinntektNår10ProsentEndring: Int,
    val månedsinntektFørsteMåned10ProsentEndring: Int,
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
        Forventet årsinntekt i ${førsteMånedMed10ProsentEndring.tilNorskFormat()}: ${forventetÅrsinntektNår10ProsentEndring.tilNorskFormat()} kroner.
            - Månedsinntekt 1/2 G: ${(Grunnbeløpsperioder.nyesteGrunnbeløp.perMnd.toInt() / 2).tilNorskFormat()} kroner
        
        Mottar uredusert stønad.
        
        Inntekten i ${førsteMånedMed10ProsentEndring.tilNorskFormat()} er ${månedsinntektFørsteMåned10ProsentEndring.tilNorskFormat()} kroner. Bruker har inntekt over 1/2 G denne måneden og alle månedene etter dette.
        Stønaden beregnes på nytt fra måneden etter inntekten oversteg 1/2 G. $feriepengerTekst
        """.trimIndent().trimEnd()
}

fun Int.tilNorskFormat(): String {
    val formatter = NumberFormat.getInstance(Locale.forLanguageTag("no-NO"))
    return formatter.format(this)
}

fun tekstTypeForGOmregningOppOgNed(
    forrigeBehandlingGOmregning: Boolean,
    forventetÅrsinntektEtterG: Int,
    tiProsentOppOgNedG: TiProsentOppOgNed,
    førsteMånedMed10ProsentEndring: YearMonth,
): String =
    if (forrigeBehandlingGOmregning && førsteMånedMed10ProsentEndring.isBefore(Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fom)) {
        """
        Forventet årsinntekt fra ${Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed.tilNorskFormat()}: ${forventetÅrsinntektEtterG.tilNorskFormat()} kroner (G-omregning).
        - 10 % opp: ${tiProsentOppOgNedG.opp.tilNorskFormat()} kroner per måned.
        - 10 % ned: ${tiProsentOppOgNedG.ned.tilNorskFormat()} kroner per måned.
        """
    } else {
        ""
    }
