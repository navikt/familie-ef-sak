package no.nav.familie.ef.sak.arbeidsfordeling

import no.nav.familie.ef.sak.infrastruktur.config.getNullable
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class ArbeidsfordelingService(private val personService: PersonService,
                              private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
                              @Qualifier("shortCache")
                              private val cacheManager: CacheManager) {

    companion object {
        const val MASKINELL_JOURNALFOERENDE_ENHET = "9999"
    }

    fun hentNavEnhet(ident: String): Arbeidsfordelingsenhet? {
        return cacheManager.getNullable("navEnhet", ident) {
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
            personopplysningerIntegrasjonerClient.hentNavEnhet(identMedStrengeste ?: ident).firstOrNull()
        }
    }

    fun hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent: String): String {
        return hentNavEnhet(personIdent)?.enhetId ?: MASKINELL_JOURNALFOERENDE_ENHET
    }

}
