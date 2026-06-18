package no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/historiskpensjon"])
class HistoriskPensjonController(
    val historiskPensjonService: HistoriskPensjonService,
    val featureToggleService: FeatureToggleService,
    val tilgangService: TilgangService,
) {
    @GetMapping("{fagsakPersonId}")
    fun hentHistoriskPensjon(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<HistoriskPensjonDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(historiskPensjonService.hentHistoriskPensjon(fagsakPersonId))
    }

    @GetMapping("fagsak/{fagsakId}")
    fun hentHistoriskPensjonForFagsak(
        @PathVariable fagsakId: UUID,
    ): Ressurs<HistoriskPensjonDto> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(historiskPensjonService.hentHistoriskPensjonForFagsak(fagsakId))
    }
}
