package no.nav.familie.ef.sak.infrastruktur.sikkerhet

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.exception.ManglerTilgang
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.AsyncHandlerInterceptor

@Component
class TilgangInterceptor(private val rolleConfig: RolleConfig) : AsyncHandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        return if (SikkerhetContext.harTilgangTilGittRolle(rolleConfig = rolleConfig, BehandlerRolle.VEILEDER)) {
            super.preHandle(request, response, handler)
        } else {
            logger.warn("Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} har ikke tilgang til saksbehandlingsløsningen")
            throw ManglerTilgang(
                melding = "Bruker har ikke tilgang til saksbehandlingsløsningen",
                frontendFeilmelding = "Du mangler tilgang til denne saksbehandlingsløsningen"
            )
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
