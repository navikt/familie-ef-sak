package no.nav.familie.ef.sak.repository.domain.søknad

data class Virksomhet(val virksomhetsbeskrivelse: String,
                      val dokumentasjon: Dokumentasjon? = null)
