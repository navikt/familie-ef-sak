package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.gui.dto.Person
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonKort
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import org.springframework.stereotype.Service

@Service
class PersonService(val integrasjonerClient: FamilieIntegrasjonerClient,
                    val pdlClient: PdlClient) {

    fun hentPerson(ident: String): Person {
        val personopplysninger = integrasjonerClient.hentPersonopplysninger(ident)
        val personhistorikk = integrasjonerClient.hentPersonhistorikk(ident)
        val pdlData = pdlClient.hentSøkerAsMap(ident)
        return Person(personopplysninger, personhistorikk, pdlData)
    }

    fun hentPdlPerson(ident: String): PdlSøker {
        return pdlClient.hentSøker(ident)
    }

    fun hentPdlPersonKort(identer: List<String>): Map<String, PdlPersonKort> {
        return identer.distinct().chunked(100).map { pdlClient.hentPersonKortBolk(it) }.reduce { acc, it -> acc + it }
    }
}
