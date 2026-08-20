package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

/**
 * Regresjonstest som verifiserer *strukturelt* at alle RestClient-bønner i RestClientConfig faktisk
 * er koblet til den samme JsonMapper-instansen som JsonMapperProvider eksponerer (bygget videre på
 * no.nav.familie.kontrakter.felles.jsonMapper).
 */
class RestClientConfigMapperErFraFellesTest {
    @TestFactory
    fun `alle RestClient-bønner i RestClientConfig bruker jsonMapper fra JsonMapperProvider felles`(): List<DynamicTest> {
        val restClientBeanFunksjoner =
            RestClientConfig::class
                .memberFunctions
                .filter { it.returnType.classifier == RestClient::class }

        assertThat(restClientBeanFunksjoner).isNotEmpty

        return restClientBeanFunksjoner.map { funksjon ->
            DynamicTest.dynamicTest(funksjon.name) {
                funksjon.isAccessible = true
                val args =
                    funksjon.parameters
                        .drop(1) // dropp "this"-parameteret
                        .associateWith { "dummy-scope" }
                val restClient = funksjon.callBy(mapOf(funksjon.parameters[0] to restClientConfig) + args) as RestClient

                val mapperBruktAvKlient = restClient.hentJsonMapper()

                assertThat(mapperBruktAvKlient)
                    .withFailMessage(
                        "RestClient-bønnen '${funksjon.name}' bruker ikke jsonMapper-instansen fra " +
                            "JsonMapperProvider (felles sin jsonMapper med KotlinPropertyNameAsImplicitName " +
                            "og øvrige moduler). Sjekk at bønnen bruker " +
                            "medJsonMapperFraFellesKontrakter() i RestClientConfig.",
                    ).isSameAs(JsonMapperProvider.jsonMapper)
            }
        }
    }

    companion object {
        private lateinit var restClientConfig: RestClientConfig

        @BeforeAll
        @JvmStatic
        fun initClass() {
            val entraIDRestClientFactory =
                mockk<EntraIDRestClientFactory> {
                    // Speiler den ekte fabrikken sin oppførsel: bygger RestClient via
                    // RestClient.builder() direkte, uten den riktige jsonMapper-en.
                    every { lagMaskinTilMaskinRestKlient(any()) } answers { RestClient.builder().build() }
                    every { lagHybridRestKlient(any(), any()) } answers { RestClient.builder().build() }
                    every { lagOboRestKlient(any(), any()) } answers { RestClient.builder().build() }
                }

            restClientConfig =
                RestClientConfig(
                    entraIDRestClientFactory = entraIDRestClientFactory,
                    consumerIdClientInterceptor = ConsumerIdClientInterceptor("familie-ef-sak", "test"),
                    mdcValuesPropagatingClientInterceptor = MdcValuesPropagatingClientInterceptor(),
                    jsonMapper = JsonMapperProvider.jsonMapper,
                )
        }
    }
}

/**
 * Henter ut JsonMapper-instansen som en bygget RestClient faktisk bruker til JSON-serialisering,
 * via refleksjon på det private feltet `messageConverters` i Spring sin `DefaultRestClient`.
 * RestClient/RestClient.Builder eksponerer ingen offentlig API for å inspisere konfigurerte
 * message-converters, så refleksjon er nødvendig for å verifisere identiteten på mapperen direkte
 * (fremfor å teste indirekte via serialiseringsoppførsel).
 */
private fun RestClient.hentJsonMapper(): JsonMapper? {
    val messageConvertersField =
        this.javaClass.getDeclaredField("messageConverters").apply { isAccessible = true }

    @Suppress("UNCHECKED_CAST")
    val converters = messageConvertersField.get(this) as List<HttpMessageConverter<*>>

    return converters
        .filterIsInstance<JacksonJsonHttpMessageConverter>()
        .firstOrNull()
        ?.mapper
}
