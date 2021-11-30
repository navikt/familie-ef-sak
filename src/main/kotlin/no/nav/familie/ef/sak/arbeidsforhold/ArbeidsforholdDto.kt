package no.nav.familie.ef.sak.arbeidsforhold

import no.nav.familie.kontrakter.felles.arbeidsforhold.ArbeidsgiverType
import no.nav.familie.kontrakter.felles.arbeidsforhold.Periode

class ArbeidsforholdDto(
    val navArbeidsforholdId: Long? = null,
    val arbeidsforholdId: String? = null,
    val arbeidstaker: ArbeidstakerDto? = null,
    val arbeidsgiver: ArbeidsgiverDto? = null,
    val type: String? = null,
    val ansettelsesperiode: AnsettelsesperiodeDto? = null,
    val arbeidsavtaler: List<ArbeidsavtaleDto>? = null
)

class ArbeidstakerDto(
    val type: String? = null,
    val offentligIdent: String? = null,
    val aktoerId: String? = null
)

class ArbeidsgiverDto(
    val type: ArbeidsgiverType? = null,
    val organisasjonsnummer: String? = null,
    val offentligIdent: String? = null
)

class AnsettelsesperiodeDto(
    val periode: Periode? = null,
    val bruksperiode: Periode? = null
)

class ArbeidsavtaleDto(
    val arbeidstidsordning: String? = null,
    val yrke: String? = null,
    val stillingsprosent: Double? = null,
    val antallTimerPrUke: Double? = null,
    val beregnetAntallTimerPrUke: Double? = null,
    val bruksperiode: Periode? = null,
    val gyldighetsperiode: Periode? = null
)

