package no.nav.familie.ef.sak.infrastruktur.config

import com.nimbusds.jwt.SignedJWT
import jakarta.servlet.Filter
import no.nav.familie.ef.sak.infrastruktur.util.MockOAuth2ServerHolder
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.context.support.TestPropertySourceUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import java.time.Instant
import java.util.Date

@SpringJUnitWebConfig(SecurityConfigTest.TestConfig::class)
@ContextConfiguration(initializers = [SecurityConfigTest.Initializer::class])
class SecurityConfigTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter<DefaultMockMvcBuilder>(context.getBean("springSecurityFilterChain", Filter::class.java))
                .build()
    }

    @ParameterizedTest
    @CsvSource(
        "azuread, /api/test, azure-audience",
        "tokenx, /api/ekstern/soknad/test, tokenx-audience",
        "tokenx, /api/ekstern/minside/test, tokenx-audience",
        "azuread, /api/task/test, azure-audience",
    )
    fun `skal avvise token med feil audience`(
        issuer: String,
        endpoint: String,
        audience: String,
    ) {
        val token = token(issuer, mapOf("aud" to "annen-tjeneste"))
        assertThat(SignedJWT.parse(token).jwtClaimsSet.audience).doesNotContain(audience)

        mockMvc
            .perform(get(endpoint).header("Authorization", "Bearer $token"))
            .andExpect(status().isUnauthorized)
    }

    @ParameterizedTest
    @CsvSource(
        "azuread, /api/test",
        "tokenx, /api/ekstern/soknad/test",
        "tokenx, /api/ekstern/minside/test",
        "azuread, /api/task/test",
    )
    fun `skal avvise token uten audience`(
        issuer: String,
        endpoint: String,
    ) {
        val token = token(issuer)
        assertThat(SignedJWT.parse(token).jwtClaimsSet.claims).doesNotContainKey("aud")

        mockMvc
            .perform(get(endpoint).header("Authorization", "Bearer $token"))
            .andExpect(status().isUnauthorized)
    }

    @ParameterizedTest
    @CsvSource(
        "azuread, /api/test, azure-audience",
        "tokenx, /api/ekstern/soknad/test, tokenx-audience",
        "tokenx, /api/ekstern/minside/test, tokenx-audience",
        "azuread, /api/task/test, azure-audience",
    )
    fun `skal godta token med riktig audience`(
        issuer: String,
        endpoint: String,
        audience: String,
    ) {
        val token = token(issuer, mapOf("aud" to audience))

        mockMvc
            .perform(get(endpoint).header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(content().string("ok"))
    }

    @ParameterizedTest
    @CsvSource(
        "azuread, /api/test, azure-audience",
        "tokenx, /api/ekstern/soknad/test, tokenx-audience",
        "azuread, /api/task/test, azure-audience",
    )
    fun `skal avvise token med feil issuer selv med riktig audience`(
        issuer: String,
        endpoint: String,
        audience: String,
    ) {
        val token = token(issuer, mapOf("aud" to audience, "iss" to "https://annen-issuer.example"))

        mockMvc
            .perform(get(endpoint).header("Authorization", "Bearer $token"))
            .andExpect(status().isUnauthorized)
    }

    @ParameterizedTest
    @CsvSource(
        "azuread, /api/test, azure-audience, exp",
        "azuread, /api/test, azure-audience, nbf",
        "tokenx, /api/ekstern/soknad/test, tokenx-audience, exp",
        "tokenx, /api/ekstern/soknad/test, tokenx-audience, nbf",
        "azuread, /api/task/test, azure-audience, exp",
        "azuread, /api/task/test, azure-audience, nbf",
    )
    fun `skal bevare levetidskontroll med riktig audience`(
        issuer: String,
        endpoint: String,
        audience: String,
        tidsclaim: String,
    ) {
        val tidspunkt = Instant.now().plusSeconds(if (tidsclaim == "exp") -120 else 120)
        val token = token(issuer, mapOf("aud" to audience, tidsclaim to Date.from(tidspunkt)))

        mockMvc
            .perform(get(endpoint).header("Authorization", "Bearer $token"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `skal fortsatt kreve riktig rolle for Azure-token med riktig audience`() {
        val token = token("azuread", mapOf("aud" to "azure-audience", "groups" to emptyList<String>()))

        mockMvc
            .perform(get("/api/test").header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
    }

    private fun token(
        issuer: String,
        claims: Map<String, Any> = emptyMap(),
    ): String =
        MockOAuth2ServerHolder.server
            .config.tokenProvider
            .jwt(
                claims =
                    mapOf(
                        "iss" to MockOAuth2ServerHolder.server.issuerUrl(issuer).toString(),
                        "sub" to "syntetisk-bruker",
                        "groups" to listOf("saksbehandler"),
                    ) + claims,
                issuerId = issuer,
            ).serialize()

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            val server = MockOAuth2ServerHolder.server
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "AZURE_OPENID_CONFIG_ISSUER=${server.issuerUrl("azuread")}",
                "TOKEN_X_ISSUER=${server.issuerUrl("tokenx")}",
                "AZURE_APP_CLIENT_ID=azure-audience",
                "TOKEN_X_CLIENT_ID=tokenx-audience",
            )
        }
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig::class, TestController::class)
    @ComponentScan("no.nav.familie.prosessering.sikkerhet")
    class TestConfig {
        @Bean
        fun azureJwtAuthenticationConverter(): AzureJwtAuthenticationConverter =
            AzureJwtAuthenticationConverter(
                RolleConfig("beslutter", "saksbehandler", "veileder", "kode6", "kode7", "egenAnsatt", "forvalter"),
            )

        @Bean
        fun prosesseringInfoProvider(): ProsesseringInfoProvider =
            object : ProsesseringInfoProvider {
                override fun hentBrukernavn(): String = "syntetisk-bruker"

                override fun harTilgang(): Boolean = true
            }
    }

    @RestController
    class TestController {
        @GetMapping("/api/test")
        @PreAuthorize("hasAuthority('ROLE_SAKSBEHANDLER')")
        fun azure(): String = "ok"

        @GetMapping("/api/ekstern/soknad/test", "/api/ekstern/minside/test", "/api/task/test")
        fun tokenxOgTask(): String = "ok"
    }
}
