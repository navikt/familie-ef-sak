package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Folkeregisteridentifikator
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapFolkeregisteridentifikator
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper.mapSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Dødsfall
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import org.springframework.stereotype.Service

data class SøkerMedBarnOgAndreForelder(
    val søker: Søker,
    val annenForelder: List<AnnenForelderMedIdent>,
    val barn: List<BarnMedIdent>
)

data class AnnenForelderMedIdent(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    val bostedsadresse: List<Bostedsadresse>,
    val dødsfall: List<Dødsfall>,
    val fødsel: List<Fødsel>,
    val navn: Navn,
    val personIdent: String,
    val folkeregisteridentifikator: List<Folkeregisteridentifikator>?
)

@Service
class GrunnlagsdataRegisterService(
    private val personService: PersonService,
) {

    fun hentGrunnlagsdataFraRegister(
        personIdent: String,
        barneforeldreFraSøknad: List<String>
    ): SøkerMedBarnOgAndreForelder {
        val pdlSøker = personService.hentSøker(personIdent)
        val pdlBarn = hentPdlBarn(pdlSøker)
        val barneForeldre = hentPdlBarneForeldre(pdlBarn, personIdent, barneforeldreFraSøknad)
        val dataTilAndreIdenter = hentDataTilAndreIdenter(pdlSøker)

        return SøkerMedBarnOgAndreForelder(
            søker = mapSøker(pdlSøker, dataTilAndreIdenter),
            annenForelder = mapAnnenForelder(barneForeldre),
            barn = mapBarn(pdlBarn),
        )
    }

    private fun mapAnnenForelder(barneForeldre: Map<String, PdlAnnenForelder>) =
        barneForeldre.map {
            AnnenForelderMedIdent(
                adressebeskyttelse = it.value.adressebeskyttelse,
                personIdent = it.key,
                fødsel = it.value.fødsel,
                bostedsadresse = it.value.bostedsadresse,
                dødsfall = it.value.dødsfall,
                navn = it.value.navn.gjeldende(),
                folkeregisteridentifikator = mapFolkeregisteridentifikator(it.value.folkeregisteridentifikator),
            )
        }

    private fun hentPdlBarn(pdlSøker: PdlSøker): Map<String, PdlPersonForelderBarn> {
        return pdlSøker.forelderBarnRelasjon
            .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
            .mapNotNull { it.relatertPersonsIdent }
            .let { personService.hentPersonForelderBarnRelasjon(it) }
    }

    private fun hentPdlBarneForeldre(
        barn: Map<String, PdlPersonForelderBarn>,
        personIdent: String,
        barneforeldrePersonIdentFraSøknad: List<String>
    ): Map<String, PdlAnnenForelder> {
        return barn.flatMap { it.value.forelderBarnRelasjon }
            .filter { it.relatertPersonsIdent != personIdent && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
            .mapNotNull { it.relatertPersonsIdent }
            .plus(barneforeldrePersonIdentFraSøknad)
            .distinct()
            .let { personService.hentAndreForeldre(it) }
    }

    private fun hentDataTilAndreIdenter(pdlSøker: PdlSøker): Map<String, PdlPersonKort> {
        val andreIdenter = pdlSøker.sivilstand.mapNotNull { it.relatertVedSivilstand } +
            pdlSøker.fullmakt.map { it.motpartsPersonident } +
            pdlSøker.vergemaalEllerFremtidsfullmakt.mapNotNull { it.vergeEllerFullmektig.motpartsPersonident }
        if (andreIdenter.isEmpty()) return emptyMap()
        return personService.hentPersonKortBolk(andreIdenter)
    }
}
