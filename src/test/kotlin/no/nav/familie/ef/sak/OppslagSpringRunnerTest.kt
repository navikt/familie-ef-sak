package no.nav.familie.ef.sak

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.familie.ef.sak.avstemming.KonsistensavstemmingJobb
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.Behandlingsjournalpost
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.migrering.Migreringsstatus
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelse
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.blankett.Blankett
import no.nav.familie.ef.sak.brev.domain.Brevmottakere
import no.nav.familie.ef.sak.brev.domain.BrevmottakereFrittståendeBrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeSanitybrev
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.database.DbContainerInitializer
import no.nav.familie.ef.sak.fagsak.domain.FagsakDomain
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.felles.util.TokenUtil
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.iverksett.oppgaveterminbarn.TerminbarnOppgave
import no.nav.familie.ef.sak.næringsinntektskontroll.NæringsinntektKontrollDomain
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknad
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.testutil.TestoppsettService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.utestengelse.Utestengelse
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.uttrekk.UttrekkArbeidssøkere
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
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [ApplicationLocalSetup::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(
    "integrasjonstest",
    "mock-arbeidssøker",
    "mock-oauth",
    "mock-pdl",
    "mock-integrasjoner",
    "mock-infotrygd-replika",
    "mock-iverksett",
    "mock-brev",
    "mock-inntekt",
    "mock-ereg",
    "mock-aareg",
    "mock-tilbakekreving",
    "mock-sigrun",
    "mock-dokument",
    "mock-historiskpensjon",
    "mock-featuretoggle",
    "mock-egen-ansatt",
    "mock-kafka",
    "mock-kontantstøtte",
    "mock-fullmakt",
)
@EnableMockOAuth2Server
abstract class OppslagSpringRunnerTest {
    protected final val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list
    protected val restTemplate = TestRestTemplate()
    protected val headers = HttpHeaders()

    @Autowired
    private lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    @Qualifier("kodeverkCache")
    private lateinit var cacheManagerKodeverk: CacheManager

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    lateinit var testoppsettService: TestoppsettService

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
            it.cacheNames
                .mapNotNull { cacheName -> it.getCache(cacheName) }
                .forEach { cache -> cache.clear() }
        }
    }

    private fun resetDatabase() {
        listOf(
            Utestengelse::class,
            UttrekkArbeidssøkere::class,
            KonsistensavstemmingJobb::class,
            Simuleringsresultat::class,
            BehandlingBarn::class,
            Søknad::class,
            SøknadsskjemaOvergangsstønad::class,
            TilkjentYtelse::class,
            Oppgave::class,
            Vilkårsvurdering::class,
            Behandlingshistorikk::class,
            Vedtaksbrev::class,
            Brevmottakere::class,
            Blankett::class,
            Vedtak::class,
            MellomlagretBrev::class,
            MellomlagretFrittståendeSanitybrev::class,
            BrevmottakereFrittståendeBrev::class,
            Behandlingsjournalpost::class,
            Grunnlagsdata::class,
            Tilbakekreving::class,
            ÅrsakRevurdering::class,
            Behandling::class,
            TerminbarnOppgave::class,
            FagsakDomain::class,
            FagsakPerson::class,
            TaskLogg::class,
            Task::class,
            Migreringsstatus::class,
            OppgaverForOpprettelse::class,
            NæringsinntektKontrollDomain::class,
        ).forEach { jdbcAggregateOperations.deleteAll(it.java) }
    }

    protected fun getPort(): String = port.toString()

    protected fun localhost(uri: String): String = LOCALHOST + getPort() + uri

    protected fun url(
        baseUrl: String,
        uri: String,
    ): String = baseUrl + uri

    protected val lokalTestToken: String
        get() {
            return onBehalfOfToken(role = rolleConfig.beslutterRolle)
        }

    protected val lokalForvalterToken: String
        get() {
            return onBehalfOfToken(roles = listOf(rolleConfig.forvalter, rolleConfig.veilederRolle))
        }

    protected fun onBehalfOfToken(
        role: String = rolleConfig.beslutterRolle,
        saksbehandler: String = "julenissen",
    ): String = onBehalfOfToken(listOf(role), saksbehandler)

    protected fun onBehalfOfToken(
        roles: List<String>,
        saksbehandler: String = "julenissen",
    ): String = TokenUtil.onBehalfOfToken(mockOAuth2Server, roles, saksbehandler)

    protected fun clientToken(
        clientId: String = "1",
        accessAsApplication: Boolean = true,
    ): String = TokenUtil.clientToken(mockOAuth2Server, clientId, accessAsApplication)

    protected fun søkerToken(
        personident: String,
        level: String = "Level4",
    ) = TokenUtil.søkerBearerToken(mockOAuth2Server, personident, level)

    companion object {
        private const val LOCALHOST = "http://localhost:"

        protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> {
            val listAppender = ListAppender<ILoggingEvent>()
            listAppender.start()
            return listAppender
        }
    }
}
