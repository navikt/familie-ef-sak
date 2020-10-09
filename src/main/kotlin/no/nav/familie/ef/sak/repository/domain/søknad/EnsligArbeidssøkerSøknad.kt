package no.nav.familie.ef.sak.repository.domain.søknad


data class EnsligArbeidssøkerSøknad(val fødselsnummer: Søknadsfelt<Fødselsnummer>,
                                    val arbeidssøker: Søknadsfelt<Arbeidssøker>)
