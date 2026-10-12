package no.nav.familie.ef.sak.infrastruktur.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.log.filter.RequestTimeFilter
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.util.StopWatch

/**
 * Maskerer sensitive deler av URI-en (eks. dokumenttittel) i vanlig logg.
 * Full URI logges kun til secureLogger.
 */
class MaskertRequestTimeFilter : RequestTimeFilter() {
    /**
     * RequestTimeFilter logger requestURI fra requesten den får inn. Den får derfor en request med maskert URI,
     * mens den originale requesten sendes videre i filterkjeden.
     */
    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain,
    ) {
        val request = servletRequest as HttpServletRequest
        val maskertUri = maskerFilnavn(request.requestURI)

        if (maskertUri == request.requestURI) {
            super.doFilter(servletRequest, servletResponse, filterChain)
            return
        }

        val timer = StopWatch().apply { start() }
        try {
            super.doFilter(RequestMedMaskertUri(request, maskertUri), servletResponse) { _, _ ->
                filterChain.doFilter(servletRequest, servletResponse)
            }
        } finally {
            timer.stop()
            loggFullUriTilSecureLogs(request, (servletResponse as HttpServletResponse).status, timer.totalTimeMillis)
        }
    }

    private fun loggFullUriTilSecureLogs(
        request: HttpServletRequest,
        status: Int,
        tidMs: Long,
    ) {
        val melding = "{} - {} - ({}). Dette tok {}ms"
        if (HttpStatusCode.valueOf(status).is5xxServerError) {
            secureLogger.warn(melding, request.method, request.requestURI, status, tidMs)
        } else {
            secureLogger.info(melding, request.method, request.requestURI, status, tidMs)
        }
    }

    private class RequestMedMaskertUri(
        request: HttpServletRequest,
        private val maskertUri: String,
    ) : HttpServletRequestWrapper(request) {
        override fun getRequestURI(): String = maskertUri
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val DOKUMENT_PDF_MED_FILNAVN = """(/dokument-pdf/[^/]+)/.+""".toRegex()

        fun maskerFilnavn(uri: String): String = DOKUMENT_PDF_MED_FILNAVN.replace(uri, "$1/***")
    }
}
