package no.nav.familie.ef.sak

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.After
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [ApplicationLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class OppslagSpringRunnerTest {

    protected val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list
    protected val restTemplate = TestRestTemplate()
    protected val headers = HttpHeaders()

    @LocalServerPort
    private var port: Int? = 0

    @After
    fun reset() {
        loggingEvents.clear()
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
