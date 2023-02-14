package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.AnnenForelderMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AnnenForelderMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.FullmaktDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.SivilstandDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.VergemålDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import org.springframework.stereotype.Component

@Component
class PersonopplysningerMapper(
    private val adresseMapper: AdresseMapper,
    private val statsborgerskapMapper: StatsborgerskapMapper,
    private val innflyttingUtflyttingMapper: InnflyttingUtflyttingMapper,
    private val arbeidsfordelingService: ArbeidsfordelingService
) {

    fun tilPersonopplysninger(
        grunnlagsdataMedMetadata: GrunnlagsdataMedMetadata,
        egenAnsatt: Boolean,
        søkerIdenter: PdlIdenter
    ): PersonopplysningerDto {
        val grunnlagsdata = grunnlagsdataMedMetadata.grunnlagsdata
        val søker = grunnlagsdata.søker
        val annenForelderMap = grunnlagsdata.annenForelder.associateBy { it.personIdent }

        val gjeldendePersonIdent = søkerIdenter.gjeldende().ident
        return PersonopplysningerDto(
            adressebeskyttelse = søker.adressebeskyttelse
                ?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
            folkeregisterpersonstatus = søker.folkeregisterpersonstatus.gjeldende()
                ?.let { Folkeregisterpersonstatus.fraPdl(it) },
            fødselsdato = søker.fødsel.gjeldende().fødselsdato,
            dødsdato = søker.dødsfall?.dødsdato,
            navn = NavnDto.fraNavn(søker.navn),
            kjønn = KjønnMapper.tilKjønn(søker.kjønn),
            personIdent = gjeldendePersonIdent,
            statsborgerskap = statsborgerskapMapper.map(søker.statsborgerskap),
            sivilstand = søker.sivilstand.map {
                SivilstandDto(
                    type = Sivilstandstype.valueOf(it.type.name),
                    gyldigFraOgMed = it.gyldigFraOgMed ?: it.bekreftelsesdato,
                    relatertVedSivilstand = it.relatertVedSivilstand,
                    navn = it.navn,
                    dødsdato = it.dødsfall?.dødsdato,
                    erGjeldende = !it.metadata.historisk
                )
            }.sortedWith(compareByDescending<SivilstandDto> { it.erGjeldende }.thenByDescending { it.gyldigFraOgMed }),
            adresse = tilAdresser(søker),
            fullmakt = søker.fullmakt.map {
                FullmaktDto(
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed,
                    motpartsPersonident = it.motpartsPersonident,
                    navn = it.navn,
                    områder = it.områder?.let { it.map { område -> mapOmråde(område) } } ?: emptyList()
                )
            }.sortedByDescending { it.gyldigFraOgMed },
            egenAnsatt = egenAnsatt,
            navEnhet = arbeidsfordelingService.hentNavEnhet(gjeldendePersonIdent)
                ?.let { it.enhetId + " - " + it.enhetNavn } ?: "Ikke funnet",
            barn = grunnlagsdata.barn.map {
                mapBarn(
                    it,
                    søkerIdenter.identer(),
                    søker.bostedsadresse,
                    annenForelderMap
                )
            }.sortedBy { it.fødselsdato },
            innflyttingTilNorge = innflyttingUtflyttingMapper.mapInnflytting(søker.innflyttingTilNorge),
            utflyttingFraNorge = innflyttingUtflyttingMapper.mapUtflytting(søker.utflyttingFraNorge),
            oppholdstillatelse = OppholdstillatelseMapper.map(søker.opphold),
            vergemål = mapVergemål(søker)
        )
    }

    private fun mapOmråde(område: String): String {
        return when (område) {
            "*" -> "ALLE"
            else -> område
        }
    }

    private fun mapVergemål(søker: Søker) =
        søker.vergemaalEllerFremtidsfullmakt.filter { it.type != "stadfestetFremtidsfullmakt" }.map {
            VergemålDto(
                embete = it.embete,
                type = it.type,
                motpartsPersonident = it.vergeEllerFullmektig.motpartsPersonident,
                navn = it.vergeEllerFullmektig.navn?.visningsnavn(),
                omfang = it.vergeEllerFullmektig.omfang
            )
        }

    fun tilAdresser(søker: Søker): List<AdresseDto> {
        val adresser =
            søker.bostedsadresse.map(adresseMapper::tilAdresse) +
                søker.kontaktadresse.map(adresseMapper::tilAdresse) +
                søker.oppholdsadresse.map(adresseMapper::tilAdresse)

        return AdresseHjelper.sorterAdresser(adresser)
    }

    fun mapBarn(
        barn: BarnMedIdent,
        søkerIdenter: Set<String>,
        bostedsadresserForelder: List<Bostedsadresse>,
        annenForelderMap: Map<String, AnnenForelderMedIdent>
    ): BarnDto {
        val annenForelderIdent = barn.forelderBarnRelasjon.find {
            !søkerIdenter.contains(it.relatertPersonsIdent) && it.relatertPersonsRolle != Familierelasjonsrolle.BARN
        }?.relatertPersonsIdent
        return BarnDto(
            personIdent = barn.personIdent,
            navn = barn.navn.visningsnavn(),
            annenForelder = annenForelderIdent?.let {
                val annenForelder = annenForelderMap[it]
                AnnenForelderMinimumDto(
                    personIdent = it,
                    navn = annenForelder?.navn?.visningsnavn() ?: "Finner ikke navn",
                    dødsdato = annenForelder?.dødsfall?.gjeldende()?.dødsdato,
                    bostedsadresse = annenForelder?.bostedsadresse?.gjeldende()
                        ?.let { adresseMapper.tilAdresse(it).visningsadresse }
                )
            },
            adresse = barn.bostedsadresse.map(adresseMapper::tilAdresse),
            borHosSøker = AdresseHjelper.borPåSammeAdresse(barn, bostedsadresserForelder),
            deltBosted = barn.deltBosted.filter { !it.metadata.historisk },
            harDeltBostedNå = AdresseHjelper.harDeltBosted(barn),
            fødselsdato = barn.fødsel.gjeldende().fødselsdato,
            dødsdato = barn.dødsfall.gjeldende()?.dødsdato
        )
    }
}
