package no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/historiskpensjon"])
@ProtectedWithClaims(issuer = "azuread")
class HistoriskPensjonController(val historiskPensjonService: HistoriskPensjonService) {

    @GetMapping("{fagsakPersonId}")
    @Cacheable("historisk_pensjon")
    fun hentFagsak(@PathVariable fagsakPersonId: UUID): Ressurs<HistoriskPensjonResponse> {
        return Ressurs.success(historiskPensjonService.hentHistoriskPensjon(fagsakPersonId))
    }
}
