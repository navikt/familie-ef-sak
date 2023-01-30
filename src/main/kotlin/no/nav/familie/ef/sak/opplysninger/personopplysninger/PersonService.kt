package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.infrastruktur.config.getCachedOrLoad
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheConfig(cacheManager = "longCache")
class PersonService(
    private val pdlClient: PdlClient,
    @Qualifier("shortCache")
    private val cacheManager: CacheManager
) {

    fun hentSøker(ident: String): PdlSøker {
        return pdlClient.hentSøker(ident)
    }

    fun hentPersonMedBarn(ident: String): SøkerMedBarn {
        val søker = hentSøker(ident)
        val barnIdentifikatorer =
            søker.forelderBarnRelasjon.filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .mapNotNull { it.relatertPersonsIdent }
        return SøkerMedBarn(ident, søker, hentPersonForelderBarnRelasjon(barnIdentifikatorer))
    }

    fun hentPersonForelderBarnRelasjon(barnIdentifikatorer: List<String>) =
        pdlClient.hentPersonForelderBarnRelasjon(barnIdentifikatorer)

    fun hentAndreForeldre(personIdenter: List<String>): Map<String, PdlAnnenForelder> {
        return pdlClient.hentAndreForeldre(personIdenter)
    }

    @Cacheable("personidenter")
    fun hentPersonIdenter(ident: String, historikk: Boolean = false): PdlIdenter =
        pdlClient.hentPersonidenter(ident = ident, historikk = historikk)

    fun hentIdenterBolk(identer: List<String>): Map<String, PdlIdent> =
        pdlClient.hentIdenterBolk(identer)

    /**
     * PDL gjør ingen tilgangskontroll i bolkoppslag, så bruker av denne metode må ha gjort tilgangskontroll
     */
    fun hentPersonKortBolk(identer: List<String>): Map<String, PdlPersonKort> {
        return cacheManager.getCachedOrLoad("pdl-person-kort-bulk", identer.distinct()) { identerUtenCache ->
            identerUtenCache.chunked(50).map { pdlClient.hentPersonKortBolk(it) }.reduce { acc, it -> acc + it }
        }
    }

    fun hentAktørIder(ident: String): PdlIdenter = pdlClient.hentAktørIder(ident)
}
