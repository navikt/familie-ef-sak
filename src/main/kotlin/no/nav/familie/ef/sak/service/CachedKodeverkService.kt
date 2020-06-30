package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class CachedKodeverkService(private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    @Cacheable("kodeverk_landkoder")
    fun hentLandkoder(): KodeverkDto {
        return familieIntegrasjonerClient.hentKodeverkLandkoder()
    }

    @Cacheable("kodeverk_poststed")
    fun hentPoststed(): KodeverkDto {
        return familieIntegrasjonerClient.hentKodeverkPoststed()
    }
}