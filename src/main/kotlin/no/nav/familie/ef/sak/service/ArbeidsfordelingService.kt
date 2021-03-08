package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.arbeidsfordeling.IdentMedAdressebeskyttelse
import no.nav.familie.ef.sak.arbeidsfordeling.finnPersonMedStrengesteAdressebeskyttelse
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import org.springframework.stereotype.Component

@Component
class ArbeidsfordelingService(private val personService: PersonService,
                              private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    private val MASKINELL_JOURNALFOERENDE_ENHET = "9999"

    fun hentNavEnhet(ident: String): Arbeidsfordelingsenhet? {
        val personMedRelasjoner = personService.hentPersonMedRelasjoner(ident)
        val søkerIdentMedAdressebeskyttelse =
                IdentMedAdressebeskyttelse(personMedRelasjoner.søkerIdent,
                                           personMedRelasjoner.søker.adressebeskyttelse.gjeldende()?.gradering)
        val identerMedAdressebeskyttelse = listOf(søkerIdentMedAdressebeskyttelse) +
                                           personMedRelasjoner.barn.map {
                                               IdentMedAdressebeskyttelse(it.key,
                                                                          it.value.adressebeskyttelse.gjeldende()?.gradering)
                                           }
        val identMedStrengeste = finnPersonMedStrengesteAdressebeskyttelse(identerMedAdressebeskyttelse)
        return familieIntegrasjonerClient.hentNavEnhet(identMedStrengeste ?: ident).firstOrNull()
    }

    fun hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent: String): String {
        return hentNavEnhet(personIdent)?.enhetId ?: MASKINELL_JOURNALFOERENDE_ENHET
    }

}
