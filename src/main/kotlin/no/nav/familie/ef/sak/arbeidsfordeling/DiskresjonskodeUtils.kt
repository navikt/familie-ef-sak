package no.nav.familie.ef.sak.arbeidsfordeling

import no.nav.familie.ef.sak.integration.dto.pdl.AdressebeskyttelseGradering

fun finnPersonMedStrengesteAdressebeskyttelse(personer: List<IdentMedAdressebeskyttelse>): String? {
    return personer.fold(null,
                         fun(person: IdentMedAdressebeskyttelse?,
                             neste: IdentMedAdressebeskyttelse): IdentMedAdressebeskyttelse? {
                             return when {
                                 person?.adressebeskyttelsegradering == AdressebeskyttelseGradering.STRENGT_FORTROLIG -> {
                                     person
                                 }
                                 neste.adressebeskyttelsegradering == AdressebeskyttelseGradering.STRENGT_FORTROLIG -> {
                                     neste
                                 }
                                 person?.adressebeskyttelsegradering == AdressebeskyttelseGradering.FORTROLIG -> {
                                     person
                                 }
                                 neste.adressebeskyttelsegradering == AdressebeskyttelseGradering.FORTROLIG
                                 -> {
                                     neste
                                 }
                                 else -> null
                             }
                         })?.ident
}