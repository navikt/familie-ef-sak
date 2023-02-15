package no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/historiskpensjon"])
@ProtectedWithClaims(issuer = "azuread")
class HistoriskPensjonController(
    val historiskPensjonService: HistoriskPensjonService,
    val featureToggleService: FeatureToggleService,
) {

    @GetMapping("{fagsakPersonId}")
    fun hentHistoriskPensjon(@PathVariable fagsakPersonId: UUID): Ressurs<HistoriskPensjonResponse> {
        return Ressurs.success(historiskPensjonService.hentHistoriskPensjon(fagsakPersonId))
    }

    @GetMapping("fagsak/{fagsakId}")
    fun hentHistoriskPensjonForFagsak(@PathVariable fagsakId: UUID): Ressurs<HistoriskPensjonResponse> {
        return Ressurs.success(historiskPensjonService.hentHistoriskPensjonForFagsak(fagsakId))
    }
}
