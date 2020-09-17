package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.integration.dto.pdl.Fullmakt
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.service.ArbeidsfordelingService
import no.nav.familie.ef.sak.service.KodeverkService
import no.nav.familie.ef.sak.util.datoEllerIdag
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PersonopplysningerMapper(private val adresseMapper: AdresseMapper,
                               private val kodeverkService: KodeverkService,
                               private val arbeidsfordelingService: ArbeidsfordelingService) {

    fun tilPersonopplysninger(søker: PdlSøker,
                              ident: String,
                              fullmakter: List<Fullmakt>,
                              egenAnsatt: Boolean,
                              identNavn: Map<String, String>): PersonopplysningerDto {
        return PersonopplysningerDto(
                adressebeskyttelse = søker.adressebeskyttelse.firstOrNull()
                        ?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
                folkeregisterpersonstatus = søker.folkeregisterpersonstatus.firstOrNull()
                        ?.let { Folkeregisterpersonstatus.fraPdl(it) },
                dødsdato = søker.dødsfall.firstOrNull()?.dødsdato,
                navn = NavnDto.fraNavn(søker.navn.gjeldende()),
                kjønn = søker.kjønn.single().kjønn.let { Kjønn.valueOf(it.name) },
                personIdent = ident,
                telefonnummer = søker.telefonnummer.find { it.prioritet == 1 }
                        ?.let { TelefonnummerDto(it.landskode, it.nummer) },
                statsborgerskap = søker.statsborgerskap.map {
                    StatsborgerskapDto(land = kodeverkService.hentLand(it.land, datoEllerIdag(it.gyldigFraOgMed)),
                                       gyldigFraOgMed = it.gyldigFraOgMed,
                                       gyldigTilOgMed = it.gyldigTilOgMed)
                },
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
                navEnhet = arbeidsfordelingService.hentNavEnhet(ident).firstOrNull()
                                   ?.let { it.enhetId + " - " + it.enhetNavn } ?: "Ej funnet"
        )
    }

    fun tilAdresser(søker: PdlSøker): List<AdresseDto> {
        val adresser =
                søker.bostedsadresse.map(adresseMapper::tilAdresse) +
                søker.kontaktadresse.map(adresseMapper::tilAdresse) +
                søker.oppholdsadresse.map(adresseMapper::tilAdresse)
        return adresser.sortedWith(compareByDescending<AdresseDto>
                                   { it.gyldigFraOgMed ?: LocalDate.MAX }
                                           .thenBy(AdresseDto::type))
    }
}