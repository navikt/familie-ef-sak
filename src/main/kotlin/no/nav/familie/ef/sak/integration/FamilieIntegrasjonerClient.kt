package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.integration.dto.familie.EgenAnsattRequest
import no.nav.familie.ef.sak.integration.dto.familie.EgenAnsattResponse
import no.nav.familie.ef.sak.integration.dto.familie.Tilgang
import no.nav.familie.ef.sak.util.medContentTypeJsonUTF8
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class FamilieIntegrasjonerClient(@Qualifier("azure") restOperations: RestOperations,
                                 private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "familie.integrasjoner") {

    override val pingUri: URI = integrasjonerConfig.pingUri
    val logger = LoggerFactory.getLogger(this::class.java)

    fun sjekkTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang {
        return postForEntity(integrasjonerConfig.tilgangRelasjonerUri, PersonIdent(personIdent), HttpHeaders().also {
            it.set("Nav-Tema", "ENF")
        })
    }

    fun hentMedlemskapsinfo(ident: String): Medlemskapsinfo {
        return postForEntity<Ressurs<Medlemskapsinfo>>(integrasjonerConfig.medlemskapUri, PersonIdent(ident)).data!!
    }

    fun hentKodeverkLandkoder(): KodeverkDto {
        return getForEntity<Ressurs<KodeverkDto>>(integrasjonerConfig.kodeverkLandkoderUri).data!!
    }

    fun hentKodeverkPoststed(): KodeverkDto {
        return getForEntity<Ressurs<KodeverkDto>>(integrasjonerConfig.kodeverkPoststedUri).data!!
    }

    fun hentNavEnhet(ident: String): List<Arbeidsfordelingsenhet> {
        val uri = integrasjonerConfig.arbeidsfordelingUri
        return try {
            val response = postForEntity<Ressurs<List<Arbeidsfordelingsenhet>>>(uri, PersonIdent(ident))
            response.data ?: throw Feil("Objektet fra integrasjonstjenesten mot arbeidsfordeling er tomt uri=$uri")
        } catch (e: RestClientException) {
            throw Feil("Kall mot integrasjon feilet ved henting av arbeidsfordelingsenhet uri=$uri", e)
        }
    }

    fun egenAnsatt(ident: String): Boolean {
        return postForEntity<Ressurs<EgenAnsattResponse>>(integrasjonerConfig.egenAnsattUri,
                                                          EgenAnsattRequest(ident)).data!!.erEgenAnsatt
    }

    fun hentInfotrygdPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        return postForEntity<Ressurs<PerioderOvergangsstønadResponse>>(integrasjonerConfig.infotrygdVedtaksperioder, request)
                .getDataOrThrow()
    }

    fun distribuerBrev(journalpostId: String): String {
        logger.info("Kaller dokdist-tjeneste for journalpost=$journalpostId")

        val journalpostRequest = DistribuerJournalpostRequest(journalpostId = journalpostId,
                                                              bestillendeFagsystem = "EF",
                                                              dokumentProdApp = "FAMILIE_EF_SAK")

        return postForEntity<Ressurs<String>>(integrasjonerConfig.distribuerDokumentUri,
                                              journalpostRequest,
                                              HttpHeaders().medContentTypeJsonUTF8()).getDataOrThrow()
    }

}
