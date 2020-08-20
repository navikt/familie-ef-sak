package no.nav.familie.ef.sak.service

import no.nav.familie.kontrakter.felles.kodeverk.hentGjeldende
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KodeverkService(private val cachedKodeverkService: CachedKodeverkService) {

    fun hentLand(landkode: String, gjeldendeDato: LocalDate): String? {
        return cachedKodeverkService.hentLandkoder().hentGjeldende(landkode, gjeldendeDato)
    }

    fun hentPoststed(postnummer: String, gjeldendeDato: LocalDate): String? {
        return cachedKodeverkService.hentPoststed().hentGjeldende(postnummer, gjeldendeDato)
    }
}
