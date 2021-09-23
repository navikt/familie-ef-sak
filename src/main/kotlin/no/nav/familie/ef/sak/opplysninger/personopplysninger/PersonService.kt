package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import org.springframework.stereotype.Service

@Service
class PersonService(private val pdlClient: PdlClient) {

    fun hentSøker(ident: String): PdlSøker {
        return pdlClient.hentSøker(ident)
    }

    fun hentPersonMedRelasjoner(ident: String): SøkerMedBarn {
        val søker = hentSøker(ident)
        val barnIdentifikatorer = søker.forelderBarnRelasjon.filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
        return SøkerMedBarn(ident, søker, pdlClient.hentBarn(barnIdentifikatorer))
    }

    fun hentPdlPersonKort(identer: List<String>): Map<String, PdlPersonKort> {
        return identer.distinct().chunked(100).map { pdlClient.hentPersonKortBolk(it) }.reduce { acc, it -> acc + it }
    }
}
