package no.nav.familie.ef.sak.repository.domain.søknad


data class SkjemaForArbeidssøker(val personaliaArbeidssøker: Søknadsfelt<PersonaliaArbeidssøker>,
                                 val arbeidssøker: Søknadsfelt<Arbeidssøker>,
                                 val innsendingsdetaljer: Søknadsfelt<Innsendingsdetaljer>)

data class PersonaliaArbeidssøker(val fødselsnummer: Søknadsfelt<Fødselsnummer>,
                                  val navn: Søknadsfelt<String>)
