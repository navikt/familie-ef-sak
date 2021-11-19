package no.nav.familie.ef.sak.arbeidsforhold

import no.nav.familie.ef.sak.arbeidsforhold.ekstern.Arbeidsforhold

fun List<Arbeidsforhold>.tilDto() = this.map {
    ArbeidsforholdDto(
        navArbeidsforholdId = it.navArbeidsforholdId,
        arbeidsforholdId = it.arbeidsforholdId,
        arbeidstaker = ArbeidstakerDto(
            it.arbeidstaker?.type,
            it.arbeidstaker?.offentligIdent,
            it.arbeidstaker?.aktoerId,
        ),
        arbeidsgiver = ArbeidsgiverDto(
            it.arbeidsgiver?.type,
            it.arbeidsgiver?.organisasjonsnummer,
            it.arbeidsgiver?.offentligIdent
        ),
        type = it.type,
        ansettelsesperiode = AnsettelsesperiodeDto(
            it.ansettelsesperiode?.periode,
            it.ansettelsesperiode?.bruksperiode
        ),
        arbeidsavtaler = it.arbeidsavtaler?.map { arbeidsavtale ->
            ArbeidsavtaleDto(
                arbeidstidsordning = arbeidsavtale.arbeidstidsordning,
                yrke = arbeidsavtale.yrke,
                stillingsprosent = arbeidsavtale.stillingsprosent,
                antallTimerPrUke = arbeidsavtale.antallTimerPrUke,
                beregnetAntallTimerPrUke = arbeidsavtale.beregnetAntallTimerPrUke,
                bruksperiode = arbeidsavtale.bruksperiode,
                gyldighetsperiode = arbeidsavtale.gyldighetsperiode
            )
        }
    )
}