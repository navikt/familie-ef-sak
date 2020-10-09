package no.nav.familie.ef.sak.repository.domain.søknad

data class Adresse(val adresse: String? = null,
                   val postnummer: String,
                   val poststedsnavn: String? = null,
                   val land: String? = null)