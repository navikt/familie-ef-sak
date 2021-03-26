package no.nav.familie.ef.sak

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.familie.ef.sak.blankett.Blankett
import no.nav.familie.ef.sak.config.RolleConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.database.DbContainerInitializer
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.onBehalfOfToken
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.repository.domain.Behandlingsjournalpost
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.ef.sak.repository.domain.Registergrunnlag
import no.nav.familie.ef.sak.repository.domain.Søknad
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [ApplicationLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrasjonstest",
                "mock-oauth",
                "mock-pdl",
                "mock-integrasjoner",
                "mock-oppdrag",
                "mock-infotrygd-replika",
                "mock-brev")
abstract class OppslagSpringRunnerTest {

    protected val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list
    protected val restTemplate = TestRestTemplate()
    protected val headers = HttpHeaders()

    @Autowired private lateinit var jdbcAggregateOperations: JdbcAggregateOperations
    @Autowired private lateinit var applicationContext: ApplicationContext
    @Autowired private lateinit var cacheManager: CacheManager
    @Autowired private lateinit var rolleConfig: RolleConfig

    @LocalServerPort
    private var port: Int? = 0

    @AfterEach
    fun reset() {
        loggingEvents.clear()
        resetDatabase()
        clearCaches()
        resetWiremockServers()
    }

    private fun resetWiremockServers() {
        applicationContext.getBeansOfType(WireMockServer::class.java).values.forEach(WireMockServer::resetRequests)
    }

    private fun clearCaches() {
        cacheManager.cacheNames.mapNotNull { cacheManager.getCache(it) }
                .forEach { it.clear() }
    }

    private fun resetDatabase() {
        listOf(
                Søknad::class,
                SøknadsskjemaOvergangsstønad::class,
                TilkjentYtelse::class,
                Oppgave::class,
                Vilkårsvurdering::class,
                Behandlingshistorikk::class,
                Registergrunnlag::class,
                Vedtaksbrev::class,
                Blankett::class,
                Vedtak::class,
                Behandlingsjournalpost::class,
                Behandling::class,
                Fagsak::class,
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
            return onBehalfOfToken(role = rolleConfig.beslutterRolle)
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
