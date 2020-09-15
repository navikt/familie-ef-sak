package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonKort
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import org.springframework.stereotype.Service

@Service
class PersonService(val pdlClient: PdlClient) {

    fun hentPdlPerson(ident: String): PdlSøker {
        return pdlClient.hentSøker(ident)
    }

    fun hentPdlPersonKort(identer: List<String>): Map<String, PdlPersonKort> {
        return identer.distinct().chunked(100).map { pdlClient.hentPersonKortBolk(it) }.reduce { acc, it -> acc + it }
    }
}
