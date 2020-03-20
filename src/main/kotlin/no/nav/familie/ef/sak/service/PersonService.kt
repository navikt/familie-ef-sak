package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.Person
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlHentPersonResponse
import org.springframework.stereotype.Service

@Service
class PersonService(val integrasjonerClient: FamilieIntegrasjonerClient,
                    val pdlClient: PdlClient) {

    fun hentPerson(ident: String): Person {
        val personopplysninger = integrasjonerClient.hentPersonopplysninger(ident)
        val personhistorikk = integrasjonerClient.hentPersonhistorikk(ident)
        return Person(personopplysninger, personhistorikk)
    }

    fun hentPdlPerson(ident: String): PdlHentPersonResponse {
        return pdlClient.hentSøker(ident)
    }
}


