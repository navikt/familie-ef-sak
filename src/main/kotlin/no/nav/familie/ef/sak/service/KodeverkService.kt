package no.nav.familie.ef.sak.service

import no.nav.familie.kontrakter.felles.kodeverk.hentGjelende
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KodeverkService(private val cachedKodeverkService: CachedKodeverkService) {

    fun hentLand(landkode: String, gjeldendeDato: LocalDate): String? {
        return cachedKodeverkService.hentLandkoder().hentGjelende(landkode, gjeldendeDato)
    }

    fun hentPoststed(postnummer: String, gjeldendeDato: LocalDate): String? {
        return cachedKodeverkService.hentPoststed().hentGjelende(postnummer, gjeldendeDato)
    }
}
