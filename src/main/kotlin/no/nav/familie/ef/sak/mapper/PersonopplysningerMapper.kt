package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.api.dto.Adressebeskyttelse
import no.nav.familie.ef.sak.api.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.domene.SøkerMedBarn
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.ef.sak.service.ArbeidsfordelingService
import no.nav.familie.ef.sak.service.KodeverkService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PersonopplysningerMapper(private val adresseMapper: AdresseMapper,
                               private val statsborgerskapMapper: StatsborgerskapMapper,
                               private val arbeidsfordelingService: ArbeidsfordelingService,
                               private val kodeverkService: KodeverkService) {

    fun tilPersonopplysninger(personMedRelasjoner: SøkerMedBarn,
                              ident: String,
                              fullmakter: List<Fullmakt>,
                              egenAnsatt: Boolean,
                              identNavn: Map<String, String>): PersonopplysningerDto {
        val søker = personMedRelasjoner.søker
        return PersonopplysningerDto(
                adressebeskyttelse = søker.adressebeskyttelse.gjeldende()
                        ?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
                folkeregisterpersonstatus = søker.folkeregisterpersonstatus.gjeldende()
                        ?.let { Folkeregisterpersonstatus.fraPdl(it) },
                dødsdato = søker.dødsfall.gjeldende()?.dødsdato,
                navn = NavnDto.fraNavn(søker.navn.gjeldende()),
                kjønn = KjønnMapper.tilKjønn(søker),
                personIdent = ident,
                telefonnummer = søker.telefonnummer.find { it.prioritet == 1 }
                        ?.let { TelefonnummerDto(it.landskode, it.nummer) },
                statsborgerskap = statsborgerskapMapper.map(søker.statsborgerskap),
                sivilstand = søker.sivilstand.map {
                    SivilstandDto(type = Sivilstandstype.valueOf(it.type.name),
                                  gyldigFraOgMed = it.gyldigFraOgMed?.toString() ?: it.bekreftelsesdato,
                                  relatertVedSivilstand = it.relatertVedSivilstand,
                                  navn = identNavn[it.relatertVedSivilstand])
                },
                adresse = tilAdresser(søker),
                fullmakt = fullmakter.map {
                    FullmaktDto(gyldigFraOgMed = it.gyldigFraOgMed,
                                gyldigTilOgMed = it.gyldigTilOgMed,
                                motpartsPersonident = it.motpartsPersonident,
                                navn = identNavn[it.motpartsPersonident])
                },
                egenAnsatt = egenAnsatt,
                navEnhet = arbeidsfordelingService.hentNavEnhet(ident)
                                   ?.let { it.enhetId + " - " + it.enhetNavn } ?: "Ikke funnet",
                barn = personMedRelasjoner.barn.map {
                    mapBarn(
                            it.key,
                            it.value,
                            personMedRelasjoner.søkerIdent,
                            personMedRelasjoner.søker.bostedsadresse,
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

    fun tilAdresser(søker: PdlSøker): List<AdresseDto> {
        val adresser =
                søker.bostedsadresse.map(adresseMapper::tilAdresse) +
                søker.kontaktadresse.map(adresseMapper::tilAdresse) +
                søker.oppholdsadresse.map(adresseMapper::tilAdresse)

        return AdresseHjelper.sorterAdresser(adresser)
    }

    fun mapBarn(personIdent: String,
                pdlBarn: PdlBarn,
                søkerIdent: String,
                bostedsadresserForelder: List<Bostedsadresse>,
                identNavnMap: Map<String, String>): BarnDto {

        val annenForelderIdent = pdlBarn.forelderBarnRelasjon.find {
            it.relatertPersonsIdent != søkerIdent && it.relatertPersonsRolle != Familierelasjonsrolle.BARN
        }?.relatertPersonsIdent
        return BarnDto(
                personIdent = personIdent,
                navn = pdlBarn.navn.gjeldende().visningsnavn(),
                annenForelder = annenForelderIdent?.let { AnnenForelderMinimumDto(it, identNavnMap[it] ?: "Finner ikke navn") },
                adresse = pdlBarn.bostedsadresse.map(adresseMapper::tilAdresse),
                borHosSøker = AdresseHjelper.borPåSammeAdresse(pdlBarn, bostedsadresserForelder),
                fødselsdato = pdlBarn.fødsel.gjeldende()?.fødselsdato
        )
    }
}