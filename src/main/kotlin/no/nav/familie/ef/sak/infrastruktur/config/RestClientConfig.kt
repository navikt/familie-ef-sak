package no.nav.familie.ef.sak.infrastruktur.config

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@Configuration
class RestClientConfig(
    private val entraIDRestClientFactory: EntraIDRestClientFactory,
    private val consumerIdClientInterceptor: ConsumerIdClientInterceptor,
    private val mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
    private val jsonMapper: JsonMapper,
) {
    private val requestFactory: ClientHttpRequestFactory =
        ClientHttpRequestFactoryBuilder
            .detect()
            .build(
                HttpClientSettings
                    .defaults()
                    .withConnectTimeout(Duration.ofSeconds(2))
                    .withReadTimeout(Duration.ofSeconds(30)),
            )

    /**
     * RestClient.builder() (og EntraIDRestClientFactory sine fabrikkmetoder) bruker Spring sin
     * default JsonMapper, som ikke har KotlinFeature.KotlinPropertyNameAsImplicitName aktivert.
     * Da blir felter med navn som starter med æ/ø/å (f.eks. årMånedFra) mistet under serialisering.
     *
     * Rotårsak: Kotlin-kompilatoren kapitaliserer ikke æ/ø/å i genererte getter-navn, så en
     * property "årMånedFra" får getteren "getårMånedFra()" (ikke "getÅrMånedFra()" som man
     * skulle forvente). Jackson 3 sin default AccessorNamingStrategy krever at basenavnet som er
     * igjen etter at "get"-prefikset er fjernet starter med stor bokstav, og forkaster derfor
     * "årMånedFra" som en gyldig property - feltet blir usynlig for serialisering. Med
     * KotlinFeature.KotlinPropertyNameAsImplicitName slått på bruker Kotlin-modulen det faktiske
     * Kotlin-egenskapsnavnet direkte i stedet for å utlede navnet fra getter-metoden, og unngår
     * dermed problemet. Vi bruker derfor eksplisitt vår egen jsonMapper, som bygger videre på
     * no.nav.familie.kontrakter.felles.jsonMapper (fra Maven-artifaktet no.nav.familie.kontrakter:felles),
     * som har denne featuren aktivert.
     */
    private fun RestClient.medJsonMapperFraFellesKontrakter(): RestClient =
        this
            .mutate()
            .configureMessageConverters { converters ->
                converters
                    .registerDefaults()
                    .withJsonConverter(JacksonJsonHttpMessageConverter(jsonMapper))
            }.build()

    private fun RestClient.medTimeout(): RestClient =
        this
            .mutate()
            .requestFactory(requestFactory)
            .build()
            .medJsonMapperFraFellesKontrakter()

    private fun hybrid(scope: String): RestClient = entraIDRestClientFactory.lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }.medTimeout()

    /** familie-integrasjoner (PersonopplysningerIntegrasjonerClient, JournalpostClient, OppgaveClient, KodeverkClient) */
    @Bean("integrasjonerRestClient")
    fun integrasjonerRestClient(
        @Value("\${FAMILIE_INTEGRASJONER_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /** PDL – systemkall (PdlClient) */
    @Bean("pdlRestClient")
    fun pdlRestClient(
        @Value("\${PDL_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope).medTimeout()

    /** PDL – på vegne av saksbehandler (PdlSaksbehandlerClient) */
    @Bean("pdlSaksbehandlerRestClient")
    fun pdlSaksbehandlerRestClient(
        @Value("\${PDL_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagOboRestKlient(scope) {
                SikkerhetContext.hentJwt()?.tokenValue ?: error("OBO-kall mot PDL uten innlogget bruker")
            }.medTimeout()

    /** repr-api / fullmakt (FullmaktClient) */
    @Bean("reprApiRestClient")
    fun reprApiRestClient(
        @Value("\${REPR_API_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /**
     * familie-ef-proxy (EregClient, ArbeidOgInntektClient, SigrunClient)
     * Kun maskin-til-maskin, appen har ingen OBO/jwt-bearer-registrering i Azure AD.
     */
    @Bean("efProxyRestClient")
    fun efProxyRestClient(
        @Value("\${FAMILIE_EF_PROXY_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope).medTimeout()

    /**
     * medlemskap MEDL (MedlClient)
     * Kun maskin-til-maskin, appen har ingen OBO/jwt-bearer-registrering i Azure AD.
     */
    @Bean("medlRestClient")
    fun medlRestClient(
        @Value("\${MEDL_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope).medTimeout()

    /** skjermede-personer (EgenAnsattClient) */
    @Bean("skjermedePersonerRestClient")
    fun skjermedePersonerRestClient(
        @Value("\${SKJERMEDE_PERSONER_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /** paw-arbeidssøkerregisteret (ArbeidssøkerClient) */
    @Bean("arbeidssokerRestClient")
    fun arbeidssokerRestClient(
        @Value("\${ARBEIDSSOKER_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /**
     * historisk-pensjon (HistoriskPensjonClient)
     * Kun maskin-til-maskin, appen har ingen OBO/jwt-bearer-registrering i Azure AD.
     */
    @Bean("historiskPensjonRestClient")
    fun historiskPensjonRestClient(
        @Value("\${HISTORISK_PENSJON_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope).medTimeout()

    /** inntektskomponenten (AMeldingInntektClient) */
    @Bean("inntektRestClient")
    fun inntektRestClient(
        @Value("\${INNTEKT_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /** familie-tilbake (TilbakekrevingClient) */
    @Bean("tilbakekrevingRestClient")
    fun tilbakekrevingRestClient(
        @Value("\${FAMILIE_TILBAKE_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /** infotrygd-replika (FSS) (InfotrygdReplikaClient) */
    @Bean("infotrygdReplikaRestClient")
    fun infotrygdReplikaRestClient(
        @Value("\${INFOTRYGD_REPLIKA_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope).medTimeout()

    /** infotrygd-replika (GCP) (InfotrygdReplikaGcpClient) */
    @Bean("infotrygdReplikaGcpRestClient")
    fun infotrygdReplikaGcpRestClient(
        @Value("\${INFOTRYGD_REPLIKA_GCP_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope).medTimeout()

    /** familie-klage (KlageClient) */
    @Bean("klageRestClient")
    fun klageRestClient(
        @Value("\${FAMILIE_KLAGE_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /**
     * aareg (ArbeidsforholdClient)
     * Kun maskin-til-maskin, appen har ingen OBO/jwt-bearer-registrering i Azure AD.
     */
    @Bean("aaregRestClient")
    fun aaregRestClient(
        @Value("\${AAREG_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope).medTimeout()

    /**
     * familie-ks-sak (KontantstøtteClient)
     * Kun maskin-til-maskin, appen har ingen OBO/jwt-bearer-registrering i Azure AD.
     */
    @Bean("ksSakRestClient")
    fun ksSakRestClient(
        @Value("\${FAMILIE_KS_SAK_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope).medTimeout()

    /**
     * AAP api-intern (ArbeidsavklaringspengerClient)
     * Kun maskin-til-maskin, appen har ingen OBO/jwt-bearer-registrering i Azure AD.
     */
    @Bean("aapRestClient")
    fun aapRestClient(
        @Value("\${ARBEIDSAVKLARINGSPENGER_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope).medTimeout()

    /** familie-ef-iverksett (IverksettClient, IverksettProxyTaskForvaltningController) */
    @Bean("iverksettRestClient")
    fun iverksettRestClient(
        @Value("\${EF_IVERKSETT_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /** Uten autentisering – for interne familie-tjenester uten auth (BrevClient, FamilieDokumentClient) */
    @Bean("utenAuthRestClient")
    fun utenAuthRestClient(): RestClient =
        RestClient
            .builder()
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(mdcValuesPropagatingClientInterceptor)
            .requestFactory(requestFactory)
            .build()
            .medJsonMapperFraFellesKontrakter()
}
