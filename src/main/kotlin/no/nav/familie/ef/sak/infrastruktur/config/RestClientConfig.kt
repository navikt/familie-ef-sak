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
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class RestClientConfig(
    private val entraIDRestClientFactory: EntraIDRestClientFactory,
    private val consumerIdClientInterceptor: ConsumerIdClientInterceptor,
    private val mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
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

    private fun RestClient.medTimeout(): RestClient = this.mutate().requestFactory(requestFactory).build()

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

    /** familie-ef-proxy (EregClient, ArbeidOgInntektClient, SigrunClient) */
    @Bean("efProxyRestClient")
    fun efProxyRestClient(
        @Value("\${FAMILIE_EF_PROXY_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /** medlemskap MEDL (MedlClient) */
    @Bean("medlRestClient")
    fun medlRestClient(
        @Value("\${MEDL_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

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

    /** historisk-pensjon (HistoriskPensjonClient) */
    @Bean("historiskPensjonRestClient")
    fun historiskPensjonRestClient(
        @Value("\${HISTORISK_PENSJON_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

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

    /** aareg (ArbeidsforholdClient) */
    @Bean("aaregRestClient")
    fun aaregRestClient(
        @Value("\${AAREG_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /** familie-ks-sak (KontantstøtteClient) */
    @Bean("ksSakRestClient")
    fun ksSakRestClient(
        @Value("\${FAMILIE_KS_SAK_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

    /** AAP api-intern (ArbeidsavklaringspengerClient) */
    @Bean("aapRestClient")
    fun aapRestClient(
        @Value("\${ARBEIDSAVKLARINGSPENGER_SCOPE}") scope: String,
    ): RestClient = hybrid(scope)

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
}
