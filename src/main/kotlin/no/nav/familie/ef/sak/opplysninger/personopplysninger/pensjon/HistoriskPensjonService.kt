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
    private val cacheManager: CacheManager,
) {
    fun hentHistoriskPensjon(fagsakPersonId: UUID): HistoriskPensjonDto {
        return hentHistoriskPensjon(fagsakPersonService.hentAktivIdent(fagsakPersonId))
    }

    fun hentHistoriskPensjonForFagsak(fagsakId: UUID): HistoriskPensjonDto {
        return hentHistoriskPensjon(fagsakService.hentAktivIdent(fagsakId))
    }

    private fun hentHistoriskPensjon(aktivIdent: String): HistoriskPensjonDto {
        val identer = personService.hentPersonIdenter(aktivIdent).identer()
        return hentHistoriskPensjon(aktivIdent, identer)
    }

    fun hentHistoriskPensjon(
        aktivIdent: String,
        identer: Set<String>,
    ): HistoriskPensjonDto {
        return cacheManager.getValue("historiskPensjon", aktivIdent) {
            historiskPensjonClient.hentHistoriskPensjonStatusForIdent(aktivIdent, identer)
        }
    }
}

data class HistoriskPensjonResponse(val harPensjonsdata: Boolean, val webAppUrl: String) {
    fun tilDto(): HistoriskPensjonDto {
        return if (harPensjonsdata) HistoriskPensjonDto(HistoriskPensjonStatus.HAR_HISTORIKK, webAppUrl) else HistoriskPensjonDto(HistoriskPensjonStatus.HAR_IKKE_HISTORIKK, webAppUrl)
    }
}

data class HistoriskPensjonDto(val historiskPensjonStatus: HistoriskPensjonStatus, val webAppUrl: String?) {
    fun harPensjonsdata(): Boolean? {
        return when (historiskPensjonStatus) {
            HistoriskPensjonStatus.UKJENT -> null
            HistoriskPensjonStatus.HAR_HISTORIKK -> true
            HistoriskPensjonStatus.HAR_IKKE_HISTORIKK -> false
        }
    }
}

enum class HistoriskPensjonStatus {
    HAR_HISTORIKK,
    HAR_IKKE_HISTORIKK,
    UKJENT,
}

data class EnsligForsoergerRequest(
    val aktivtFoedselsnummer: String,
    val alleRelaterteFoedselsnummer: Set<String>,
)
