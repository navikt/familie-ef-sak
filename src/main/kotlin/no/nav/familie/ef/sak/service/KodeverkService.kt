package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.hentGjelende
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KodeverkService(private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    fun hentLand(landkode: String, gjeldendeDato: LocalDate): String? {
        return hentLandkoder().hentGjelende(landkode, gjeldendeDato)
    }

    fun hentPoststed(postnummer: String, gjeldendeDato: LocalDate): String? {
        return hentPoststed().hentGjelende(postnummer, gjeldendeDato)
    }

    @Cacheable("kodeverk_landkoder")
    fun hentLandkoder(): KodeverkDto {
        return familieIntegrasjonerClient.hentKodeverkLandkoder()
    }

    @Cacheable("kodeverk_poststed")
    fun hentPoststed(): KodeverkDto {
        return familieIntegrasjonerClient.hentKodeverkPoststed()
    }
}
