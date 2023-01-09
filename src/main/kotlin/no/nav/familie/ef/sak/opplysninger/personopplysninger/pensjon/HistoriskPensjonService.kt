package no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon

import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheConfig(cacheManager = "historiskPensjonCache")
class HistoriskPensjonService(private val historiskPensjonClient: HistoriskPensjonClient) {

    @Cacheable("historisk_pensjon")
    fun hentHistoriskPensjon(personIdent: String): HistoriskPensjonResponse {
        return historiskPensjonClient.harPensjon(personIdent)
    }
}

data class HistoriskPensjonResponse(val harPensjonsdata: Boolean, val webAppUrl: String)
data class HistoriskPensjonRequest(val fnr: String)
