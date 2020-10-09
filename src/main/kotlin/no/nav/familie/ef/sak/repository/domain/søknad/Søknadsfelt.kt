package no.nav.familie.ef.sak.repository.domain.søknad

data class Søknadsfelt<T>(val label: String,
                          val verdi: T)
