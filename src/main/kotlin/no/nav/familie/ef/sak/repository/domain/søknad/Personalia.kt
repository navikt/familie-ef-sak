package no.nav.familie.ef.sak.repository.domain.søknad

data class Personalia(val fødselsnummer: Fødselsnummer,
                      val navn: String,
                      val statsborgerskap: String,
                      val adresse: Adresse,
                      val telefonnummer: String? = null,
                      val sivilstatus: String)

