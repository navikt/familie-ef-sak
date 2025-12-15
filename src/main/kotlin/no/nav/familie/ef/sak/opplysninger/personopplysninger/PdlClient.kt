package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.infrastruktur.config.PdlConfig
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBolkResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlHentIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdentBolkRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdentBolkRequestVariables
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdentBolkResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdentRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdentRequestVariables
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonBolkRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonBolkRequestVariables
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonRequestVariables
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøkerData
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Tema
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class PdlClient(
    val pdlConfig: PdlConfig,
    @Qualifier("azureClientCredential") restTemplate: RestOperations,
) : AbstractPingableRestClient(restTemplate, "pdl.personinfo") {
    override val pingUri: URI
        get() = pdlConfig.pdlUri

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun hentSøker(personIdent: String): PdlSøker {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(personIdent),
                query = PdlConfig.søkerQuery,
            )
        val pdlResponse: PdlResponse<PdlSøkerData> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlPersonRequest,
                httpHeaders(),
            )
        return feilsjekkOgReturnerData(personIdent, pdlResponse) { it.person }
    }

    fun hentPersonForelderBarnRelasjon(personIdenter: List<String>): Map<String, PdlPersonForelderBarn> {
        if (personIdenter.isEmpty()) return emptyMap()
        val pdlPersonRequest =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personIdenter),
                query = PdlConfig.forelderBarnQuery,
            )
        val pdlResponse: PdlBolkResponse<PdlPersonForelderBarn> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlPersonRequest,
                httpHeaders(),
            )
        return feilsjekkOgReturnerData(pdlResponse)
    }

    fun hentAndreForeldre(personIdenter: List<String>): Map<String, PdlAnnenForelder> {
        if (personIdenter.isEmpty()) return emptyMap()
        val pdlPersonRequest =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personIdenter),
                query = PdlConfig.annenForelderQuery,
            )
        val pdlResponse: PdlBolkResponse<PdlAnnenForelder> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlPersonRequest,
                httpHeaders(),
            )
        return feilsjekkOgReturnerData(pdlResponse)
    }

    fun hentPersonKortBolk(personIdenter: List<String>): Map<String, PdlPersonKort> {
        require(personIdenter.size <= 100) { "Liste med personidenter må være færre enn 100 st" }
        val pdlPersonRequest =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personIdenter),
                query = PdlConfig.personBolkKortQuery,
            )
        val pdlResponse: PdlBolkResponse<PdlPersonKort> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlPersonRequest,
                httpHeaders(),
            )
        return feilsjekkOgReturnerData(pdlResponse)
    }

    /**
     * @param ident Ident til personen, samme hvilke type (Folkeregisterident, aktørid eller npid)
     * @return liste med aktørider
     */
    fun hentAktørIder(ident: String): PdlIdenter {
        val pdlPersonRequest =
            PdlIdentRequest(
                variables = PdlIdentRequestVariables(ident, "AKTORID"),
                query = PdlConfig.hentIdentQuery,
            )
        val pdlResponse: PdlResponse<PdlHentIdenter> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlPersonRequest,
                httpHeaders(),
            )
        return feilsjekkOgReturnerData(ident, pdlResponse) { it.hentIdenter }
    }

    /**
     * @param ident Ident til personen, samme hvilke type (Folkeregisterident, aktørid eller npid)
     * @return liste med folkeregisteridenter
     */
    fun hentPersonidenter(ident: String): PdlIdenter {
        val pdlIdentRequest =
            PdlIdentRequest(
                variables = PdlIdentRequestVariables(ident, "FOLKEREGISTERIDENT", historikk = true),
                query = PdlConfig.hentIdentQuery,
            )
        val pdlResponse: PdlResponse<PdlHentIdenter> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlIdentRequest,
                httpHeaders(),
            )
        val pdlIdenter = feilsjekkOgReturnerData(ident, pdlResponse) { it.hentIdenter }

        if (pdlIdenter.identer.isEmpty()) {
            logger.error("Finner ikke personidenter for personIdent i PDL $ident ")
        }
        return pdlIdenter
    }

    /**
     * @param identer Identene til personene, samme hvilke type (Folkeregisterident, aktørid eller npid).
     * For tiden (2020-03-22) maks 100 identer lovlig i spørring.
     * @return map med søkeident som nøkkel og liste av folkeregisteridenter
     */
    fun hentIdenterBolk(identer: List<String>): Map<String, PdlIdent> {
        feilHvis(identer.size > MAKS_ANTALL_IDENTER) {
            "Feil i spørring mot PDL. Antall identer i spørring overstiger $MAKS_ANTALL_IDENTER"
        }
        val pdlIdentBolkRequest =
            PdlIdentBolkRequest(
                variables = PdlIdentBolkRequestVariables(identer, "FOLKEREGISTERIDENT"),
                query = PdlConfig.hentIdenterBolkQuery,
            )
        val pdlResponse: PdlIdentBolkResponse =
            postForEntity(
                pdlConfig.pdlUri,
                pdlIdentBolkRequest,
                httpHeaders(),
            )

        return feilmeldOgReturnerData(pdlResponse)
    }

    private fun httpHeaders(): HttpHeaders =
        HttpHeaders().apply {
            add("Tema", "ENF")
            add("behandlingsnummer", Tema.ENF.behandlingsnummer)
        }

    companion object {
        const val MAKS_ANTALL_IDENTER = 100
    }
}
