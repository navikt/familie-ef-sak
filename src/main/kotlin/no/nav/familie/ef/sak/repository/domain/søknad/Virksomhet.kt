package no.nav.familie.ef.sak.repository.domain.søknad

data class Virksomhet(val virksomhetsbeskrivelse: Søknadsfelt<String>,
                      val dokumentasjon: Søknadsfelt<Dokumentasjon>? = null)
