package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.AdresseDto
import no.nav.familie.ef.sak.api.dto.Adressebeskyttelse
import no.nav.familie.ef.sak.api.dto.AnnenForelderMinimumDto
import no.nav.familie.ef.sak.api.dto.BarnDto
import no.nav.familie.ef.sak.api.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.api.dto.FullmaktDto
import no.nav.familie.ef.sak.api.dto.InnflyttingDto
import no.nav.familie.ef.sak.api.dto.NavnDto
import no.nav.familie.ef.sak.api.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.api.dto.SivilstandDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.api.dto.TelefonnummerDto
import no.nav.familie.ef.sak.api.dto.UtflyttingDto
import no.nav.familie.ef.sak.domene.BarnMedIdent
import no.nav.familie.ef.sak.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.domene.Søker
import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.service.ArbeidsfordelingService
import no.nav.familie.ef.sak.service.KodeverkService
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
        val identNavn = grunnlagsdata.annenForelder.associate { it.personIdent to it.navn.visningsnavn() }

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
                                  navn = it.navn)
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
                            identNavn)
                },
                innflyttingTilNorge = søker.innflyttingTilNorge.map {
                    InnflyttingDto(fraflyttingsland = it.fraflyttingsland?.let { kodeverkService.hentLand(it, LocalDate.now()) },
                                   dato = null,
                                   fraflyttingssted = it.fraflyttingsstedIUtlandet)
                },
                utflyttingFraNorge = søker.utflyttingFraNorge.map {
                    UtflyttingDto(tilflyttingsland = it.tilflyttingsland?.let { kodeverkService.hentLand(it, LocalDate.now()) },
                                  dato = null,
                                  tilflyttingssted = it.tilflyttingsstedIUtlandet)
                },
                oppholdstillatelse = OppholdstillatelseMapper.map(søker.opphold)
        )
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
                identNavnMap: Map<String, String>): BarnDto {

        val annenForelderIdent = barn.forelderBarnRelasjon.find {
            it.relatertPersonsIdent != søkerIdent && it.relatertPersonsRolle != Familierelasjonsrolle.BARN
        }?.relatertPersonsIdent
        return BarnDto(
                personIdent = barn.personIdent,
                navn = barn.navn.visningsnavn(),
                annenForelder = annenForelderIdent?.let { AnnenForelderMinimumDto(it, identNavnMap[it] ?: "Finner ikke navn") },
                adresse = barn.bostedsadresse.map(adresseMapper::tilAdresse),
                borHosSøker = AdresseHjelper.borPåSammeAdresse(barn, bostedsadresserForelder),
                fødselsdato = barn.fødsel.gjeldende().fødselsdato
        )
    }
}