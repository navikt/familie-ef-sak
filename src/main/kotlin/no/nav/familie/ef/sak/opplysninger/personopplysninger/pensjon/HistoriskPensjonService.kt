package no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheConfig(cacheManager = "longCache")
class HistoriskPensjonService(
    private val historiskPensjonClient: HistoriskPensjonClient,
    val fagsakPersonService: FagsakPersonService,
    val personService: PersonService
) {

    @Cacheable
    fun hentHistoriskPensjon(fagsakPersonId: UUID): HistoriskPensjonResponse {
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val identer = personService.hentPersonIdenter(aktivIdent).identer()
        return hentHistoriskPensjon(aktivIdent, identer)
    }

    @Cacheable("hentHistoriskPensjon_aktivIdent")
    fun hentHistoriskPensjon(
        aktivIdent: String,
        identer: Set<String>,
    ) = historiskPensjonClient.harPensjon(aktivIdent, identer)
}

data class HistoriskPensjonResponse(val harPensjonsdata: Boolean, val webAppUrl: String)
data class EnsligForsoergerRequest(
    val aktivtFoedselsnummer: String,
    val alleRelaterteFoedselsnummer: Set<String>
)
