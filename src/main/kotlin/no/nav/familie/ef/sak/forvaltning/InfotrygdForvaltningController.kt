package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("rapport")
    fun hentRapportÅpneSaker(): Ressurs<InfotrygdReplikaClient.ÅpnesakerRapport> {
        logger.info("Henter åpne saker fra infotrygd")
        feilHvisIkke(tilgangService.harForvalterrolle()) { "Må være forvalter for å hente ut rapport" }
        return Ressurs.success(infotrygdService.hentÅpneSaker())
    }
}
