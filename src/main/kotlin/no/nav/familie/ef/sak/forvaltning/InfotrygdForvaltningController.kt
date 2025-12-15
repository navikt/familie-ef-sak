package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvaltning/infotrygd")
@ProtectedWithClaims(issuer = "azuread")
class InfotrygdForvaltningController(
    private val infotrygdService: InfotrygdService,
    private val tilgangService: TilgangService,
) {
    val logger = Logg.getLogger(this::class)

    @GetMapping("rapport")
    fun hentRapportÅpneSaker(): Ressurs<InfotrygdReplikaClient.ÅpnesakerRapport> {
        logger.info("Henter åpne saker fra infotrygd")
        tilgangService.validerHarForvalterrolle()
        return Ressurs.success(infotrygdService.hentÅpneSaker())
    }
}
