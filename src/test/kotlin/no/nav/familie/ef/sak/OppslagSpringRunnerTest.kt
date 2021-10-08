package no.nav.familie.ef.sak

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.familie.ef.sak.avstemming.KonsistensavstemmingJobb
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.Behandlingsjournalpost
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.blankett.Blankett
import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.database.DbContainerInitializer
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.TokenUtil
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknad
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.Vedtak
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
                "mock-infotrygd-replika",
                "mock-iverksett",
                "mock-brev")
@EnableMockOAuth2Server
abstract class OppslagSpringRunnerTest {

    protected val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list
    protected val restTemplate = TestRestTemplate()
    protected val headers = HttpHeaders()

    @Autowired private lateinit var jdbcAggregateOperations: JdbcAggregateOperations
    @Autowired private lateinit var applicationContext: ApplicationContext
    @Autowired private lateinit var cacheManager: CacheManager
    @Autowired @Qualifier("kodeverkCache") private lateinit var cacheManagerKodeverk: CacheManager
    @Autowired private lateinit var rolleConfig: RolleConfig
    @Autowired private lateinit var mockOAuth2Server: MockOAuth2Server

    @LocalServerPort
    private var port: Int? = 0

    @AfterEach
    fun reset() {
        headers.clear()
        loggingEvents.clear()
        resetDatabase()
        clearCaches()
        resetWiremockServers()
    }

    private fun resetWiremockServers() {
        applicationContext.getBeansOfType(WireMockServer::class.java).values.forEach(WireMockServer::resetRequests)
    }

    private fun clearCaches() {
        listOf(cacheManagerKodeverk, cacheManager).forEach {
            it.cacheNames.mapNotNull { cacheName -> it.getCache(cacheName) }
                    .forEach { cache -> cache.clear() }
        }
    }

    private fun resetDatabase() {
        listOf(KonsistensavstemmingJobb::class,
               Simuleringsresultat::class,
               Søknad::class,
               SøknadsskjemaOvergangsstønad::class,
               TilkjentYtelse::class,
               Oppgave::class,
               Vilkårsvurdering::class,
               Behandlingshistorikk::class,
               Vedtaksbrev::class,
               Blankett::class,
               Vedtak::class,
               MellomlagretBrev::class,
               Behandlingsjournalpost::class,
               Grunnlagsdata::class,
               Behandling::class,
               Fagsak::class,
               TaskLogg::class,
               Task::class
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

    protected fun onBehalfOfToken(role: String = rolleConfig.beslutterRolle, saksbehandler: String = "julenissen"): String {
        return TokenUtil.onBehalfOfToken(mockOAuth2Server, role, saksbehandler)
    }

    protected fun clientToken(clientId: String = "1", accessAsApplication: Boolean = true): String {
        return TokenUtil.clientToken(mockOAuth2Server, clientId, accessAsApplication)
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
