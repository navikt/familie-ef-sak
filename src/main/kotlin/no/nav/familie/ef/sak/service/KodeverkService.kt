package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.integrasjoner.kodeverk.domene.KodeverkDto
import no.nav.familie.integrasjoner.kodeverk.domene.Språk
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KodeverkService(private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    fun LocalDate.mellom(fra: LocalDate, til: LocalDate) =
            this.isEqual(fra) || this.isEqual(til) || (this.isAfter(fra) && this.isBefore(til))

    fun KodeverkDto.hentGjelende(kode: String, gjeldendeDato: LocalDate) =
            betydninger[kode]
                    ?.firstOrNull { gjeldendeDato.mellom(it.gyldigFra, it.gyldigTil) }
                    ?.beskrivelser?.get(Språk.BOKMÅL.kode)?.term

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
