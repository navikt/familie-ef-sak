package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.api.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.integration.dto.pdl.*
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PersonopplysningerService(private val personService: PersonService) {

    fun hentPdlSøker(ident: String): PersonopplysningerDto {
        val søker = personService.hentPdlPerson(ident)
        val fullmakter = søker.fullmakt.filter { it.motpartsRolle == MotpartsRolle.FULLMEKTIG }
        val sivilstand = søker.sivilstand
        //val andreIdenter = fullmakter.map { it.motpartsPersonident } + sivilstand.map { it.relatertVedSivilstand }.filterNotNull() //TODO hent personinfo
        return PersonopplysningerDto(
                statsborgerskap = søker.statsborgerskap.map {
                    StatsborgerskapDto(land = it.land,
                                       gyldigFraOgMed = it.gyldigFraOgMed,
                                       gyldigTilOgMed = it.gyldigTilOgMed)
                },
                folkeregisterpersonstatus = Folkeregisterpersonstatus.fraPdl(søker.folkeregisterpersonstatus.single()),
                navn = NavnDto.fraNavn(søker.navn.gjeldende()),
                sivilstand = sivilstand.map {
                    SivilstandDto(type = Sivilstandstype.valueOf(it.type.name),
                                  gyldigFraOgMed = it.gyldigFraOgMed,
                                  relatertVedSivilstand = it.relatertVedSivilstand,
                                  navn = null) //TODO
                },
                telefonnummer = søker.telefonnummer.find { it.prioritet == 1 }
                        ?.let { TelefonnummerDto(it.landskode, it.nummer) },
                adresse = adresse(søker),
                fullmakt = fullmakter.map {
                    FullmaktDto(gyldigFraOgMed = it.gyldigFraOgMed,
                                gyldigTilOgMed = it.gyldigTilOgMed,
                                motpartsPersonident = it.motpartsPersonident,
                                navn = null) //TODO
                }
        )
    }

    private fun adresse(søker: PdlSøker): List<AdresseDto> {
        val adresser =
                søker.bostedsadresse.map {
                    AdresseDto(visningsadresse = it.tilFormatertAdresse(),
                               type = AdresseType.BOSTEDADRESSE,
                               gyldigFraOgMed = it.angittFlyttedato,
                               gyldigTilOgMed = it.folkeregistermetadata.opphørstidspunkt?.toLocalDate())
                } +
                søker.kontaktadresse.map {
                    val type = when (it.type) {
                        KontaktadresseType.INNLAND -> AdresseType.KONTAKTADRESSE
                        KontaktadresseType.UTLAND -> AdresseType.KONTAKTADRESSE_UTLAND
                    }
                    AdresseDto(visningsadresse = it.tilFormatertAdresse(),
                               type = type,
                               gyldigFraOgMed = it.gyldigFraOgMed,
                               gyldigTilOgMed = it.gyldigTilOgMed)
                } +
                søker.oppholdsadresse.map {
                    AdresseDto(visningsadresse = it.tilFormatertAdresse(),
                               type = AdresseType.OPPHOLDSADRESSE,
                               gyldigFraOgMed = it.oppholdsadressedato,
                               gyldigTilOgMed = null)
                }
        return adresser.sortedWith(compareByDescending<AdresseDto>
                                   { it.gyldigFraOgMed ?: LocalDate.MAX }
                                           .thenBy(AdresseDto::type))
    }
}