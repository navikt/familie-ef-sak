package no.nav.familie.ef.sak

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ApplicationLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class OppslagSpringRunnerTest {

    protected val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list
    protected val restTemplate = TestRestTemplate()
    protected val headers = HttpHeaders()

    @Autowired private lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    @LocalServerPort
    private var port: Int? = 0

    @AfterEach
    fun reset() {
        loggingEvents.clear()
        resetDatabase()
    }

    private fun resetDatabase() {
        listOf(Vedlegg::class,
               Sak::class,
               TilkjentYtelse::class,
               Oppgave::class,
               Behandling::class,
               FagsakPerson::class,
               Fagsak::class
        ).forEach { jdbcAggregateOperations.deleteAll(it.java) }
    }

    protected fun getPort(): String {
        return port.toString()
    }

    protected fun localhost(uri: String): String {
        return LOCALHOST + getPort() + uri
    }

    protected fun url(baseUrl: String, uri: String): String {
        return baseUrl + uri
    }

    protected val lokalTestToken: String
        get() {
            val cookie = restTemplate.exchange(localhost("/local/cookie"),
                                               HttpMethod.GET,
                                               HttpEntity.EMPTY,
                                               String::class.java)
            return tokenFraRespons(cookie)
        }

    private fun tokenFraRespons(cookie: ResponseEntity<String>): String {
        return cookie.body!!.split("value\":\"".toRegex()).toTypedArray()[1].split("\"".toRegex()).toTypedArray()[0]
    }

    companion object {
        private const val LOCALHOST = "http://localhost:"
        protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> {
            val listAppender = ListAppender<ILoggingEvent>()
            listAppender.start()
            return listAppender
        }
    }
}
