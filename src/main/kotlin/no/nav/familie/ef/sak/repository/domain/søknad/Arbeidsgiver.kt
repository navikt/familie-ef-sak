package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate


data class Arbeidsgiver(val arbeidsgivernavn: String,
                        val arbeidsmengde: Int? = null,
                        val fastEllerMidlertidig: String,
                        val harSluttdato: Boolean?,
                        val sluttdato: LocalDate? = null)

/**
 * Arbeidsmengde skal ikke fylles ut av Barnetilsyn
 */
