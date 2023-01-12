package no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheConfig(cacheManager = "historiskPensjonCache")
class HistoriskPensjonService(private val historiskPensjonClient: HistoriskPensjonClient, val fagsakPersonService: FagsakPersonService) {

    @Cacheable("historisk_pensjon")
    fun hentHistoriskPensjon(fagsakPersonId: UUID): HistoriskPensjonResponse {
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        return historiskPensjonClient.harPensjon(aktivIdent)
    }
}

data class HistoriskPensjonResponse(val harPensjonsdata: Boolean, val webAppUrl: String)
data class HistoriskPensjonRequest(val fnr: String)
