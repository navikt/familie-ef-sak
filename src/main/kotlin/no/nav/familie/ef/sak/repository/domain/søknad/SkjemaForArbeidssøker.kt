package no.nav.familie.ef.sak.repository.domain.søknad


data class SkjemaForArbeidssøker(val personaliaArbeidssøker: PersonaliaArbeidssøker,
                                 val arbeidssøker: Arbeidssøker,
                                 val innsendingsdetaljer: Innsendingsdetaljer)

data class PersonaliaArbeidssøker(val fødselsnummer: Fødselsnummer,
                                  val navn: String)
