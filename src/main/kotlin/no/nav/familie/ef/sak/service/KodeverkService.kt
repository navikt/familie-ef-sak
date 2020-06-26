package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.hentGjelende
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KodeverkService(private val cachedKodeverkService: CachedKodeverkService) {

    /**
     *  En metode som ikke er @Cacheable i en bean som kaller en annen metode som har @Cacheable bruker aldri cached
     */
    @Service
    class CachedKodeverkService(private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

        @Cacheable("kodeverk_landkoder", sync = true)
        fun hentLandkoder(): KodeverkDto {
            return familieIntegrasjonerClient.hentKodeverkLandkoder()
        }

        @Cacheable("kodeverk_poststed", sync = true)
        fun hentPoststed(): KodeverkDto {
            return familieIntegrasjonerClient.hentKodeverkPoststed()
        }
    }

    fun hentLand(landkode: String, gjeldendeDato: LocalDate): String? {
        return cachedKodeverkService.hentLandkoder().hentGjelende(landkode, gjeldendeDato)
    }

    fun hentPoststed(postnummer: String, gjeldendeDato: LocalDate): String? {
        return cachedKodeverkService.hentPoststed().hentGjelende(postnummer, gjeldendeDato)
    }
}
