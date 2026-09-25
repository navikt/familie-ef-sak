package no.nav.familie.ef.sak.infrastruktur.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import jakarta.servlet.http.HttpServletRequest
import no.nav.familie.log.filter.RequestTimeFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

internal class MaskertRequestTimeFilterTest {
    private val filter = MaskertRequestTimeFilter()
    private val uri = "/api/journalpost/765211771/dokument-pdf/803824457/Husleiekontrakt%20(barnefar)"

    private val logger = LoggerFactory.getLogger(RequestTimeFilter::class.java) as Logger
    private val logAppender = ListAppender<ILoggingEvent>()
    private val secureLogger = LoggerFactory.getLogger("secureLogger") as Logger
    private val secureLogAppender = ListAppender<ILoggingEvent>()

    @BeforeEach
    internal fun setUp() {
        logAppender.start()
        logger.addAppender(logAppender)
        secureLogAppender.start()
        secureLogger.addAppender(secureLogAppender)
    }

    @AfterEach
    internal fun tearDown() {
        logger.detachAppender(logAppender)
        secureLogger.detachAppender(secureLogAppender)
    }

    @ParameterizedTest
    @CsvSource("200, INFO", "500, WARN")
    internal fun `skal maskere filnavn i vanlig logg, logge full uri til secureLogger og sende original request videre`(
        status: Int,
        forventetNivå: String,
    ) {
        val request = MockHttpServletRequest("GET", uri)
        val response = MockHttpServletResponse().apply { this.status = status }
        val filterChain = MockFilterChain()

        filter.doFilter(request, response, filterChain)

        assertThat(logAppender.list).hasSize(1)
        assertThat(logAppender.list.single().formattedMessage)
            .contains("/api/journalpost/765211771/dokument-pdf/803824457/***")
            .doesNotContain("Husleiekontrakt")

        assertThat(secureLogAppender.list).hasSize(1)
        assertThat(secureLogAppender.list.single().level).isEqualTo(Level.toLevel(forventetNivå))
        assertThat(secureLogAppender.list.single().formattedMessage)
            .isEqualTo("GET - $uri - ($status). Dette tok ${secureLogAppender.list.single().argumentArray[3]}ms")

        assertThat((filterChain.request as HttpServletRequest).requestURI).isEqualTo(uri)
    }

    @Test
    internal fun `skal ikke logge til secureLogger når uri ikke inneholder sensitive data`() {
        val uriUtenFilnavn = "/api/journalpost/765211771/dokument-pdf/803824457"
        val filterChain = MockFilterChain()

        filter.doFilter(MockHttpServletRequest("GET", uriUtenFilnavn), MockHttpServletResponse(), filterChain)

        assertThat(logAppender.list.single().formattedMessage).contains(uriUtenFilnavn)
        assertThat(secureLogAppender.list).isEmpty()
        assertThat((filterChain.request as HttpServletRequest).requestURI).isEqualTo(uriUtenFilnavn)
    }

    @Test
    internal fun `skal maskere filnavn i dokument-pdf-uri`() {
        assertThat(MaskertRequestTimeFilter.maskerFilnavn(uri))
            .isEqualTo("/api/journalpost/765211771/dokument-pdf/803824457/***")
    }

    @Test
    internal fun `skal ikke endre dokument-pdf-uri uten filnavn`() {
        val uriUtenFilnavn = "/api/journalpost/765211771/dokument-pdf/803824457"

        assertThat(MaskertRequestTimeFilter.maskerFilnavn(uriUtenFilnavn)).isEqualTo(uriUtenFilnavn)
    }

    @Test
    internal fun `skal ikke endre andre uri-er`() {
        val annenUri = "/api/journalpost/765211771/dokument/803824457"

        assertThat(MaskertRequestTimeFilter.maskerFilnavn(annenUri)).isEqualTo(annenUri)
    }
}
