package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.domene.SøkerMedBarn
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonKort
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import org.springframework.stereotype.Service

@Service
class PersonService(val pdlClient: PdlClient) {

    fun hentSøker(ident: String): PdlSøker {
        return pdlClient.hentSøker(ident)
    }

    fun hentPersonMedRelasjoner(ident: String): SøkerMedBarn {
        val søker = hentSøker(ident)
        val barnIdentifikatorer = søker.familierelasjoner.filter { it.minRolleForPerson == Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
        return SøkerMedBarn(ident, søker, pdlClient.hentBarn(barnIdentifikatorer))
    }

    fun hentPdlPersonKort(identer: List<String>): Map<String, PdlPersonKort> {
        return identer.distinct().chunked(100).map { pdlClient.hentPersonKortBolk(it) }.reduce { acc, it -> acc + it }
    }
}
