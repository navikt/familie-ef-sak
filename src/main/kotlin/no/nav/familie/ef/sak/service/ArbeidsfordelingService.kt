package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.arbeidsfordeling.IdentMedAdressebeskyttelse
import no.nav.familie.ef.sak.arbeidsfordeling.finnPersonMedStrengesteAdressebeskyttelse
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import org.springframework.stereotype.Component

@Component
class ArbeidsfordelingService(private val personService: PersonService,
                              private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    fun hentNavEnhet(ident: String): List<Arbeidsfordelingsenhet> {
        val personMedRelasjoner = personService.hentPersonMedRelasjoner(ident)
        val søkerIdentMedAdressebeskyttelse = IdentMedAdressebeskyttelse(personMedRelasjoner.søkerIdent,
                                                                         personMedRelasjoner.søker.adressebeskyttelse.firstOrNull()?.gradering)
        val identerMedAdressebeskyttelse = listOf(søkerIdentMedAdressebeskyttelse) +
                                           personMedRelasjoner.barn.map {
                                               IdentMedAdressebeskyttelse(it.key,
                                                                          it.value.adressebeskyttelse.firstOrNull()?.gradering)
                                           }
        val identMedStrengeste = finnPersonMedStrengesteAdressebeskyttelse(identerMedAdressebeskyttelse)
        return try {
            familieIntegrasjonerClient.hentNavEnhet(identMedStrengeste ?: ident)
        } catch (e: Exception) {
            listOf(Arbeidsfordelingsenhet("0", "Henting av Nav-enhet feilet"))
        }
    }

}
