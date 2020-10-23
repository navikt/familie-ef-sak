package no.nav.familie.ef.sak.sikkerhet

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.config.RolleConfig
import no.nav.familie.ef.sak.service.steg.BehandlerRolle
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Import(RolleConfig::class)
class TilgangInterceptor(private val rolleConfig: RolleConfig) : HandlerInterceptorAdapter() {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        return if (SikkerhetContext.harTilgangTilGittRolle(rolleConfig = rolleConfig, BehandlerRolle.VEILEDER)) {
            super.preHandle(request, response, handler)
        } else {
            logger.warn("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang til saksbehandlingsløsningen")
            throw Feil(
                    "Bruker har ikke tilgang til saksbehandlingsløsningen",
                    "Bruker har ikke tilgang til saksbehandlingsløsningen",
                    httpStatus = HttpStatus.FORBIDDEN
            )
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(this::class.java)
    }

}