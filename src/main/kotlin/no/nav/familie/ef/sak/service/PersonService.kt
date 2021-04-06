package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.domene.SøkerMedBarn
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonKort
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
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

    fun hentAndreForeldre(identer: List<String>): Map<String, PdlAnnenForelder> {
        return pdlClient.hentAndreForeldre(identer)
    }

    fun hentIdenterForBarnOgForeldre(forelderIdent: String): List<String>{
        val søkerMedBarn = hentPersonMedRelasjoner(forelderIdent)

        val forelderIdenter = søkerMedBarn.barn.values
                .flatMap { it.forelderBarnRelasjon }
                .filter { it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }

        val barnIdenter = søkerMedBarn.barn.keys

        return (forelderIdenter + barnIdenter + forelderIdent).distinct()
    }
}
