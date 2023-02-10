package no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.config.getValue
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HistoriskPensjonService(
    private val historiskPensjonClient: HistoriskPensjonClient,
    private val fagsakPersonService: FagsakPersonService,
    private val fagsakService: FagsakService,
    private val personService: PersonService,
    @Qualifier("longCache")
    private val cacheManager: CacheManager
) {

    fun hentHistoriskPensjon(fagsakPersonId: UUID): HistoriskPensjonResponse {
        return hentHistoriskPensjon(fagsakPersonService.hentAktivIdent(fagsakPersonId))
    }

    fun hentHistoriskPensjonForFagsak(fagsakId: UUID): HistoriskPensjonResponse {
        return hentHistoriskPensjon(fagsakService.hentAktivIdent(fagsakId))
    }

    private fun hentHistoriskPensjon(aktivIdent: String): HistoriskPensjonResponse {
        val identer = personService.hentPersonIdenter(aktivIdent).identer()
        return hentHistoriskPensjon(aktivIdent, identer)
    }

    fun hentHistoriskPensjon(
        aktivIdent: String,
        identer: Set<String>
    ): HistoriskPensjonResponse {
        return cacheManager.getValue("historiskPensjon", aktivIdent) {
            historiskPensjonClient.harPensjon(aktivIdent, identer)
        }
    }
}

data class HistoriskPensjonResponse(val harPensjonsdata: Boolean, val webAppUrl: String)
data class EnsligForsoergerRequest(
    val aktivtFoedselsnummer: String,
    val alleRelaterteFoedselsnummer: Set<String>
)
