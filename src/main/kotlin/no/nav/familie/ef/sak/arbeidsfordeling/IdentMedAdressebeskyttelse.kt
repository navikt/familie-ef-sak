package no.nav.familie.ef.sak.arbeidsfordeling

import no.nav.familie.ef.sak.integration.dto.pdl.AdressebeskyttelseGradering

data class IdentMedAdressebeskyttelse(
        val ident: String,
        val adressebeskyttelsegradering: AdressebeskyttelseGradering?
)
