package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.felles.kodeverk.KodeverkService
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
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.InnflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.SivilstandDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.TelefonnummerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.UtflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.VergemålDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PersonopplysningerMapper(private val adresseMapper: AdresseMapper,
                               private val statsborgerskapMapper: StatsborgerskapMapper,
                               private val arbeidsfordelingService: ArbeidsfordelingService,
                               private val kodeverkService: KodeverkService) {

    fun tilPersonopplysninger(grunnlagsdataMedMetadata: GrunnlagsdataMedMetadata,
                              egenAnsatt: Boolean,
                              ident: String): PersonopplysningerDto {
        val grunnlagsdata = grunnlagsdataMedMetadata.grunnlagsdata
        val søker = grunnlagsdata.søker
        val annenForelderMap = grunnlagsdata.annenForelder.associateBy { it.personIdent }

        return PersonopplysningerDto(
                lagtTilEtterFerdigstilling = grunnlagsdataMedMetadata.lagtTilEtterFerdigstilling,
                adressebeskyttelse = søker.adressebeskyttelse
                        ?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
                folkeregisterpersonstatus = søker.folkeregisterpersonstatus.gjeldende()
                        ?.let { Folkeregisterpersonstatus.fraPdl(it) },
                dødsdato = søker.dødsfall?.dødsdato,
                navn = NavnDto.fraNavn(søker.navn),
                kjønn = KjønnMapper.tilKjønn(søker.kjønn),
                personIdent = ident,
                telefonnummer = søker.telefonnummer.find { it.prioritet == 1 }
                        ?.let { TelefonnummerDto(it.landskode, it.nummer) },
                statsborgerskap = statsborgerskapMapper.map(søker.statsborgerskap),
                sivilstand = søker.sivilstand.map {
                    SivilstandDto(type = Sivilstandstype.valueOf(it.type.name),
                                  gyldigFraOgMed = it.gyldigFraOgMed?.toString() ?: it.bekreftelsesdato,
                                  relatertVedSivilstand = it.relatertVedSivilstand,
                                  navn = it.navn,
                                  dødsdato = it.dødsfall?.dødsdato)
                },
                adresse = tilAdresser(søker),
                fullmakt = søker.fullmakt.map {
                    FullmaktDto(gyldigFraOgMed = it.gyldigFraOgMed,
                                gyldigTilOgMed = it.gyldigTilOgMed,
                                motpartsPersonident = it.motpartsPersonident,
                                navn = it.navn)
                },
                egenAnsatt = egenAnsatt,
                navEnhet = arbeidsfordelingService.hentNavEnhet(ident)
                                   ?.let { it.enhetId + " - " + it.enhetNavn } ?: "Ikke funnet",
                barn = grunnlagsdata.barn.map {
                    mapBarn(it,
                            ident,
                            søker.bostedsadresse,
                            annenForelderMap)
                },
                innflyttingTilNorge = søker.innflyttingTilNorge.map {
                    InnflyttingDto(it.fraflyttingsland?.let { land -> kodeverkService.hentLand(land, LocalDate.now()) },
                                   null,
                                   it.fraflyttingsstedIUtlandet)
                },
                utflyttingFraNorge = søker.utflyttingFraNorge.map {
                    UtflyttingDto(it.tilflyttingsland?.let { land -> kodeverkService.hentLand(land, LocalDate.now()) },
                                  null,
                                  it.tilflyttingsstedIUtlandet)
                },
                oppholdstillatelse = OppholdstillatelseMapper.map(søker.opphold),
                vergemål = mapVergemål(søker)
        )
    }

    private fun mapVergemål(søker: Søker) =
            søker.vergemaalEllerFremtidsfullmakt.filter { it.type != "stadfestetFremtidsfullmakt" }.map {
                VergemålDto(embete = it.embete,
                            type = it.type,
                            motpartsPersonident = it.vergeEllerFullmektig.motpartsPersonident,
                            navn = it.vergeEllerFullmektig.navn?.visningsnavn(),
                            omfang = it.vergeEllerFullmektig.omfang)
            }

    fun tilAdresser(søker: Søker): List<AdresseDto> {
        val adresser =
                søker.bostedsadresse.map(adresseMapper::tilAdresse) +
                søker.kontaktadresse.map(adresseMapper::tilAdresse) +
                søker.oppholdsadresse.map(adresseMapper::tilAdresse)

        return AdresseHjelper.sorterAdresser(adresser)
    }

    fun mapBarn(barn: BarnMedIdent,
                søkerIdent: String,
                bostedsadresserForelder: List<Bostedsadresse>,
                annenForelderMap: Map<String, AnnenForelderMedIdent>): BarnDto {

        val annenForelderIdent = barn.forelderBarnRelasjon.find {
            it.relatertPersonsIdent != søkerIdent && it.relatertPersonsRolle != Familierelasjonsrolle.BARN
        }?.relatertPersonsIdent
        return BarnDto(personIdent = barn.personIdent,
                       navn = barn.navn.visningsnavn(),
                       annenForelder = annenForelderIdent?.let {
                           AnnenForelderMinimumDto(personIdent = it,
                                                   navn = annenForelderMap[it]?.navn?.visningsnavn() ?: "Finner ikke navn",
                                                   dødsdato = annenForelderMap[it]?.dødsfall?.gjeldende()?.dødsdato)
                       },
                       adresse = barn.bostedsadresse.map(adresseMapper::tilAdresse),
                       borHosSøker = AdresseHjelper.borPåSammeAdresse(barn, bostedsadresserForelder),
                       fødselsdato = barn.fødsel.gjeldende().fødselsdato,
                       dødsdato = barn.dødsfall.gjeldende()?.dødsdato)
    }
}