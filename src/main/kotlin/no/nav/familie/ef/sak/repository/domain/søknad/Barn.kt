package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate

data class Barn(val navn: String? = null,
                val fødselsnummer: Fødselsnummer? = null,
                val harSkalHaSammeAdresse: Boolean,
                val ikkeRegistrertPåSøkersAdresseBeskrivelse: String?,
                val erBarnetFødt: Boolean,
                val fødselTermindato: LocalDate? = null,
                val terminbekreftelse: Dokumentasjon? = null,
                val annenForelder: AnnenForelder? = null,
                val samvær: Samvær? = null,
                val skalHaBarnepass: Boolean? = null,
                val særligeTilsynsbehov: String? = null,
                val barnepass: Barnepass? = null)

/**
 * skalHaBarnepass, barnepass gjelder Barnetilsyn
 */
