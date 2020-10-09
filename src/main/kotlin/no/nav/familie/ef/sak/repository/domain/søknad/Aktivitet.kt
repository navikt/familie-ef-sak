package no.nav.familie.ef.sak.repository.domain.søknad

data class Aktivitet(val hvordanErArbeidssituasjonen: List<String>,
                     val arbeidsforhold: List<Arbeidsgiver>? = null,
                     @Deprecated("Bruk firmaer istedenfor") val selvstendig: Selvstendig? = null,
                     val firmaer: List<Selvstendig>? = null,
                     val virksomhet: Virksomhet? = null,
                     val arbeidssøker: Arbeidssøker? = null,
                     val underUtdanning: UnderUtdanning? = null,
                     val aksjeselskap: List<Aksjeselskap>? = null,
                     val erIArbeid: String? = null,
                     val erIArbeidDokumentasjon: Dokumentasjon? = null)

/**
 * erIArbeid, erIArbeidDokumentasjon: gjelder Barnetilsyn
 */
