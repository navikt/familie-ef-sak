package no.nav.familie.ef.sak.felles.kodeverk

import no.nav.familie.kontrakter.felles.kodeverk.hentGjeldende
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KodeverkService(
    private val cachedKodeverkService: CachedKodeverkService,
) {
    fun hentLand(
        landkode: String,
        gjeldendeDato: LocalDate,
    ): String? = cachedKodeverkService.hentLandkoder().hentGjeldende(landkode, gjeldendeDato, sisteGjeldende = true)

    fun hentPoststed(
        postnummer: String,
        gjeldendeDato: LocalDate,
    ): String? = cachedKodeverkService.hentPoststed().hentGjeldende(postnummer, gjeldendeDato, sisteGjeldende = true)
}
